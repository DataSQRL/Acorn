package com.datasqrl.ai.backend;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatSession<Message, FunctionCall> {

  private final FunctionBackend backend;
  private final Map<String, Object> sessionContext;
  private final String systemMessage;
  private final ModelBindings<Message, FunctionCall> bindings;
  private final List<GenericChatMessage> messages = new ArrayList<>();

  public ChatSession(FunctionBackend backend, Map<String, Object> sessionContext, String systemMessage,
                     ModelBindings<Message, FunctionCall> bindings) {
    this.backend = backend;
    this.sessionContext = sessionContext;
    this.systemMessage = systemMessage;
    this.bindings = bindings;
    messages.addAll(backend.getChatMessages(sessionContext, Integer.MAX_VALUE, GenericChatMessage.class));
  }
  public GenericChatMessage addMessage(Message message) {
    GenericChatMessage convertedMsg = bindings.convertMessage(message, sessionContext);
    messages.add(convertedMsg);
    backend.saveChatMessage(convertedMsg);
    return convertedMsg;
  }
  public List<Message> getHistory(int limit) {
    return this.messages.stream().limit(limit).map(bindings::convertMessage).toList();
  }
  public List<Message> getHistory() {
    return this.getHistory(Integer.MAX_VALUE);
  }
  protected ContextWindow<GenericChatMessage> getContextWindow(int maxTokens, ModelAnalyzer<Message> analyzer) {
    GenericChatMessage systemMessage = bindings.createSystemMessage(this.systemMessage, sessionContext);
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
    if (numMessages > 0) System.out.printf("Truncated the first %s messages\n", numMessages);
    return window;
  }

  public ContextWindow<Message> getContextWindow() {
    ContextWindow<GenericChatMessage> context = getContextWindow(bindings.getMaxInputTokens(), bindings.getTokenCounter());
    return new ContextWindow<>(context.getMessages().stream().map(bindings::convertMessage).toList(), context.getFunctions(), context.getNumTokens());
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

}
