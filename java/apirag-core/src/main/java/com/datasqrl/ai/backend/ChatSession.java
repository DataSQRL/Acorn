package com.datasqrl.ai.backend;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatSession<Message, FunctionCall> {

  private static final int MESSAGE_HISTORY_LIMIT = 100;

  private final FunctionBackend backend;
  private final Map<String, Object> context;
  private final String systemMessage;
  private final ModelBindings<Message, FunctionCall> bindings;
  private final List<GenericChatMessage> messages = new ArrayList<>();

  public ChatSession(FunctionBackend backend, Map<String, Object> context, String systemMessage,
                     ModelBindings<Message, FunctionCall> bindings) {
    this.backend = backend;
    this.context = context;
    this.systemMessage = systemMessage;
    this.bindings = bindings;
    List<GenericChatMessage> chatHistory = backend.getChatMessages(context, MESSAGE_HISTORY_LIMIT, GenericChatMessage.class);
    log.info("Retrieved {} messages from history", chatHistory.size());
    messages.addAll(chatHistory);
  }

  public GenericChatMessage addMessage(Message message) {
    GenericChatMessage convertedMsg = bindings.convertMessage(message, context);
    messages.add(convertedMsg);
    backend.saveChatMessage(convertedMsg);
    return convertedMsg;
  }

  protected ContextWindow<GenericChatMessage> getContextWindow(int maxTokens, ModelAnalyzer<Message> analyzer) {
    GenericChatMessage systemMessage = bindings.convertMessage(bindings.createSystemMessage(this.systemMessage), context);
    final AtomicInteger numTokens = new AtomicInteger(0);
    numTokens.addAndGet(systemMessage.getNumTokens());
    ContextWindow.ContextWindowBuilder<GenericChatMessage> builder = ContextWindow.builder();
    backend.getFunctions().values().stream().map(f -> {
      numTokens.addAndGet(f.getNumTokens(analyzer));
      return f.getChatFunction();
    }).forEach(builder::function);
    if (numTokens.get() > maxTokens)
      throw new IllegalArgumentException("Function calls and system message too large for model: " + numTokens);
    int numMessages = messages.size();
    List<GenericChatMessage> resultMessages = new ArrayList<>();
    ListIterator<GenericChatMessage> listIterator = messages.listIterator(numMessages);
    while (listIterator.hasPrevious()) {
      GenericChatMessage message = listIterator.previous();
      numTokens.addAndGet(message.getNumTokens(msg -> analyzer.countTokens(bindings.convertMessage(msg))));
      if (numTokens.get() > maxTokens) break;
      resultMessages.add(message);
      numMessages--;
    }
    builder.message(systemMessage);
    Collections.reverse(resultMessages);
    builder.messages(resultMessages);
    builder.numTokens(numTokens.get());
    ContextWindow<GenericChatMessage> window = builder.build();
    if (numMessages > 0) log.info("Truncated the first {} messages", numMessages);
    return window;
  }

  public ContextWindow<Message> getContextWindow() {
    ContextWindow<GenericChatMessage> context = getContextWindow(bindings.getMaxInputTokens(), bindings.getTokenCounter());
    return new ContextWindow<>(context.getMessages().stream().map(bindings::convertMessage).toList(),
        context.getFunctions(), context.getNumTokens());
  }

  public FunctionValidation<Message> validateFunctionCall(FunctionCall chatFunctionCall) {
    return backend.validateFunctionCall(bindings.getFunctionName(chatFunctionCall),
        bindings.getFunctionArguments(chatFunctionCall)).translate(bindings::convertExceptionToMessage);
  }

  public Message executeFunctionCall(FunctionCall chatFunctionCall, Map<String, Object> context) {
    String functionName = bindings.getFunctionName(chatFunctionCall);
    JsonNode functionArguments = bindings.getFunctionArguments(chatFunctionCall);
    try {
      String functionResult = backend.executeFunctionCall(functionName, functionArguments, context);
      return bindings.newFunctionResultMessage(functionName, functionResult);
    } catch (Exception e) {
      return convertExceptionToMessage(e);
    }
  }

  private Message convertExceptionToMessage(Exception exception) {
    String error = exception.getMessage() == null ? exception.toString() : exception.getMessage();
    return bindings.convertExceptionToMessage(error);
  }

  public FunctionExecutionOutcome validateAndExecuteFunctionCall(FunctionCall functionCall) {
    FunctionValidation<String> fctValid = backend.validateFunctionCall(bindings.getFunctionName(functionCall),
        bindings.getFunctionArguments(functionCall));
    if (fctValid.isValid()) {
      if (fctValid.isClientExecuted()) { //return as is - evaluated on frontend
        return new FunctionExecutionOutcome(FunctionExecutionOutcome.Status.EXECUTE_ON_CLIENT, null);
      } else {
        String functionName = bindings.getFunctionName(functionCall);
        log.info("Executing {} with arguments {}", functionName,
            bindings.getFunctionArguments(functionCall).toPrettyString());
        Message functionResponse = this.executeFunctionCall(functionCall, context);
        log.info("Executed {} with results: {}" , functionName, bindings.getTextContent(functionResponse));
        this.addMessage(functionResponse);
        return new FunctionExecutionOutcome(FunctionExecutionOutcome.Status.EXECUTED, null);
      }
    } else {
      Message retryResponse = bindings.newUserMessage("It looks like you tried to call a function, but this has failed with the following error: "
          + fctValid.validationError() + ". Please retry to call the function again.");
      this.addMessage(retryResponse);
      return new FunctionExecutionOutcome(FunctionExecutionOutcome.Status.VALIDATION_ERROR_RETRY, fctValid.validationError());
    }
  }

  public record FunctionExecutionOutcome(
      Status status,
      FunctionValidation.ValidationError<String> validationError
  ) {
    public enum Status {
      EXECUTED,
      EXECUTE_ON_CLIENT,
      EXECUTION_ERROR,
      VALIDATION_ERROR_RETRY
    }
  }
}
