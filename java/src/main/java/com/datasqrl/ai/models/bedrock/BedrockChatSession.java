package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.backend.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

public class BedrockChatSession extends AbstractChatSession<BedrockChatMessage, BedrockFunctionCall> {

  BedrockChatModel chatModel;
  BedrockTokenCounter tokenCounter;

  private final String FUNCTION_CALLING_PROMPT = "To call a function, respond only with a JSON object of the following format: "
      + "{\"function\": \"$FUNCTION_NAME\","
      + "  \"parameters\": {"
      + "  \"$PARAMETER_NAME1\": \"$PARAMETER_VALUE1\","
      + "  \"$PARAMETER_NAME2\": \"$PARAMETER_VALUE2\","
      + "  ..."
      + "}. "
      + "You will get a function response from the user in the following format: "
      + "{\"function_response\" : \"$FUNCTION_NAME\","
      + "  \"data\": { \"$RESULT_TYPE\": [$RESULTS] }"
      + "}\n"
      + "Here are the functions you can use:";

  public BedrockChatSession(BedrockChatModel model,
                            BedrockChatMessage systemMessage,
                            FunctionBackend backend,
                            Map<String, Object> sessionContext) {
    super(backend, sessionContext, null);
    this.chatModel = model;
    this.tokenCounter = BedrockTokenCounter.of(model);
    this.systemMessage = convertMessage(combineSystemPromptAndFunctions(systemMessage.getTextContent()));
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
      return new BedrockChatMessage(BedrockChatRole.FUNCTION,
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
      fctCall = msg.getFunctionCall();
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

  private BedrockChatMessage combineSystemPromptAndFunctions(String systemPrompt) {
    ObjectMapper objectMapper = new ObjectMapper();
//    Note: This approach does not take into account the context window for the system prompt
    String functionText = this.FUNCTION_CALLING_PROMPT
        + this.backend.getFunctions().values().stream()
        .map(RuntimeFunctionDefinition::getChatFunction)
        .map(f ->
            objectMapper.createObjectNode()
                .put("type", "function")
                .set("function", objectMapper.valueToTree(f))
        )
        .map(value -> {
          try {
            return objectMapper.writeValueAsString(value);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        })
        .collect(Collectors.joining("\n"));
    return new BedrockChatMessage(BedrockChatRole.SYSTEM, systemPrompt + "\n" + functionText + "\n", "");
  }

  private static String functionCall2String(BedrockFunctionCall fctCall) {
    return "{"
        + "\"function\": \"" + fctCall.getFunctionName() + "\", "
        + "\"parameters\": " + fctCall.getArguments().toString()
        + "}";
  }

}
