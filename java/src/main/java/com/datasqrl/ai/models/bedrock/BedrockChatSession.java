package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.backend.AbstractChatSession;
import com.datasqrl.ai.backend.ChatSessionComponents;
import com.datasqrl.ai.backend.ContextWindow;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.backend.GenericChatMessage;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

public class BedrockChatSession extends AbstractChatSession<BedrockChatMessage, BedrockFunctionCall> {

  BedrockChatModel chatModel;
  BedrockTokenCounter tokenCounter;

  public BedrockChatSession(BedrockChatModel model, BedrockChatMessage systemMessage,
                            FunctionBackend backend,
                            Map<String, Object> sessionContext) {
    super(backend, sessionContext, null);
    this.chatModel = model;
    this.tokenCounter = BedrockTokenCounter.of(model);
    this.systemMessage = convertMessage(systemMessage);
  }

  @Override
  public ChatSessionComponents<BedrockChatMessage> getSessionComponents() {
    ContextWindow<GenericChatMessage> context = getWindow(chatModel.getMaxInputTokens(), tokenCounter);
    return new ChatSessionComponents<>(context.getMessages().stream().map(this::convertMessage).collect(
        Collectors.toUnmodifiableList()), context.getFunctions());
  }

  private BedrockChatMessage convertExceptionToMessage(Exception exception) {
    String error = exception.getMessage() == null ? exception.toString() : exception.getMessage();
    return convertExceptionToMessage(error);
  }

  private BedrockChatMessage convertExceptionToMessage(String error) {
    return new BedrockChatMessage(BedrockChatRole.USER, "{\"error\": \"" + error + "\"}", "error");
  }

  @Override
  public FunctionValidation<BedrockChatMessage> validateFunctionCall(BedrockFunctionCall chatFunctionCall) {
    return backend.validateFunctionCall(chatFunctionCall.getFunctionName(),
        chatFunctionCall.getArguments()).translate(this::convertExceptionToMessage);
  }

  @Override
  public BedrockChatMessage executeFunctionCall(BedrockFunctionCall chatFunctionCall) {
    try {
      return new BedrockChatMessage(BedrockChatRole.USER,
          backend.executeFunctionCall(chatFunctionCall.getFunctionName(), chatFunctionCall.getArguments(), sessionContext),
          chatFunctionCall.getFunctionName());
    } catch (Exception e) {
      return convertExceptionToMessage(e);
    }
  }

  @Override
  protected BedrockChatMessage convertMessage(GenericChatMessage message) {
    BedrockChatRole role = BedrockChatRole.valueOf(message.getRole().toUpperCase());
    //Parse function call?
    return switch (role) {
      case SYSTEM -> new BedrockChatMessage(BedrockChatRole.SYSTEM, message.getContent(), message.getName());
      case USER -> new BedrockChatMessage(BedrockChatRole.USER, message.getContent(), message.getName());
      case ASSISTANT -> new BedrockChatMessage(BedrockChatRole.ASSISTANT, message.getContent(), message.getName());
      case FUNCTION -> new BedrockChatMessage(BedrockChatRole.FUNCTION, message.getContent(), message.getName());
    };
  }


  @Override
  protected GenericChatMessage convertMessage(BedrockChatMessage msg) {
    BedrockFunctionCall fctCall = null;
    if (msg.getRole() == BedrockChatRole.ASSISTANT) {
      fctCall = getFunctionCallFromMessage(msg);
    }
    return GenericChatMessage.builder()
        .role(msg.getRole().getRole())
        .content(fctCall == null ? msg.getTextContent() : functionCall2String(fctCall))
        .name(msg.getName())
        .context(sessionContext)
        .timestamp(Instant.now().toString())
        .numTokens(tokenCounter.countTokens(msg))
        .build();
  }

  private BedrockFunctionCall getFunctionCallFromMessage(BedrockChatMessage msg) {
    BedrockFunctionCall functionCall = null;
    String textContent = msg.getTextContent();
    if (textContent.contains("{\"function\":")) {
      int start = textContent.indexOf("{\"function\":");
      int end = textContent.lastIndexOf("{");
      String jsonContent = textContent.substring(start, end);
      System.out.println(jsonContent);
    }
    return functionCall;
  }

  private static String functionCall2String(BedrockFunctionCall fctCall) {
    return fctCall.getFunctionName() + "@" + fctCall.getArguments().toPrettyString();
  }

}
