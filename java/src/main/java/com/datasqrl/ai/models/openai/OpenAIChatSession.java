package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.backend.AbstractChatSession;
import com.datasqrl.ai.backend.ContextWindow;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.backend.GenericChatMessage;
import com.theokanning.openai.completion.chat.*;

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


  private FunctionMessage convertExceptionToMessage(Exception exception) {
    String error = exception.getMessage() == null ? exception.toString() : exception.getMessage();
    return convertExceptionToMessage(error);
  }

  private FunctionMessage convertExceptionToMessage(String error) {
    return new FunctionMessage("{\"error\": \"" + error + "\"}", "error");
  }

  @Override
  public FunctionValidation<ChatMessage> validateFunctionCall(ChatFunctionCall chatFunctionCall) {
    return backend.validateFunctionCall(chatFunctionCall.getName(),
        chatFunctionCall.getArguments()).translate(this::convertExceptionToMessage);
  }

  @Override
  public FunctionMessage executeFunctionCall(ChatFunctionCall chatFunctionCall) {
    try {
      return new FunctionMessage(
          backend.executeFunctionCall(chatFunctionCall.getName(), chatFunctionCall.getArguments(), sessionContext),
          chatFunctionCall.getName());
    } catch (Exception e) {
      return convertExceptionToMessage(e);
    }
  }

  @Override
  protected ChatMessage convertMessage(GenericChatMessage message) {
    ChatMessageRole role = ChatMessageRole.valueOf(message.getRole().toUpperCase());
      //Parse function call?
    return switch(role) {
      case SYSTEM -> new SystemMessage(message.getContent(), message.getName());
      case USER -> new UserMessage(message.getContent(), message.getName());
      case ASSISTANT -> new AssistantMessage(message.getContent(), message.getName());
      case FUNCTION -> new FunctionMessage(message.getContent(), message.getName());
      case TOOL -> new ToolMessage(message.getContent(), message.getName());
    };
  }


  @Override
  protected GenericChatMessage convertMessage(ChatMessage msg) {
    ChatFunctionCall fctCall = null;
    if (ChatMessageRole.valueOf(msg.getRole().toUpperCase()) == ChatMessageRole.ASSISTANT) {
      fctCall = ((AssistantMessage) msg).getFunctionCall();
    }
    return GenericChatMessage.builder()
        .role(msg.getRole())
        .content(fctCall == null? msg.getTextContent(): functionCall2String(fctCall))
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
