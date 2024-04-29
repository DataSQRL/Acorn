package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.AbstractChatSession;
import com.datasqrl.ai.backend.ContextWindow;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.backend.GenericChatMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenAIChatSession extends AbstractChatSession<ChatMessage, ChatFunctionCall> {

  ChatModel chatModel;
  OpenAITokenCounter tokenCounter;

  public OpenAIChatSession(ChatModel model, ChatMessage systemMessage,
      FunctionBackend backend,
      Map<String, Object> sessionContext) {
    super(backend, sessionContext, null);
    this.chatModel = model;
    this.tokenCounter = OpenAITokenCounter.of(model);
    this.systemMessage = convertMessage(systemMessage);
  }

  public ChatCompletionRequest.ChatCompletionRequestBuilder setContext(ChatCompletionRequest.ChatCompletionRequestBuilder requestBuilder) {
    ContextWindow<GenericChatMessage> context = getWindow(chatModel.getMaxInputTokens(), tokenCounter);
    requestBuilder.messages(context.getMessages().stream().map(this::convertMessage).collect(
        Collectors.toUnmodifiableList()));
    requestBuilder.functions(context.getFunctions());
    return requestBuilder;
  }


  private ChatMessage convertExceptionToMessage(Exception exception) {
    String error = exception.getMessage() == null ? exception.toString() : exception.getMessage();
    return convertExceptionToMessage(error);
  }

  private ChatMessage convertExceptionToMessage(String error) {
    return new ChatMessage(ChatMessageRole.FUNCTION.value(), "{\"error\": \"" + error + "\"}", "error");
  }

  @Override
  public FunctionValidation<ChatMessage> validateFunctionCall(ChatFunctionCall chatFunctionCall) {
    return backend.validateFunctionCall(chatFunctionCall.getName(),
        chatFunctionCall.getArguments()).translate(this::convertExceptionToMessage);
  }

  @Override
  public ChatMessage executeFunctionCall(ChatFunctionCall chatFunctionCall) {
    try {
      return new ChatMessage(ChatMessageRole.FUNCTION.value(),
          backend.executeFunctionCall(chatFunctionCall.getName(), chatFunctionCall.getArguments(), sessionContext),
          chatFunctionCall.getName());
    } catch (Exception e) {
      return convertExceptionToMessage(e);
    }
  }

  @Override
  protected ChatMessage convertMessage(GenericChatMessage message) {
    ChatMessage chatMessage = new ChatMessage(message.getRole(), message.getContent(), message.getName());
    //Parse function call?
    return chatMessage;
  }

  @Override
  protected GenericChatMessage convertMessage(ChatMessage msg) {
    return GenericChatMessage.builder()
        .role(msg.getRole())
        .content(msg.getFunctionCall() == null? msg.getContent(): functionCall2String(msg.getFunctionCall()))
        .name(msg.getName())
        .context(sessionContext)
        .timestamp(Instant.now().toString())
        .numTokens(tokenCounter.countTokens(msg))
        .build();
  }

  private static String functionCall2String(ChatFunctionCall fctCall) {
    return fctCall.getName() + "@" + fctCall.getArguments().toPrettyString();
  }

}
