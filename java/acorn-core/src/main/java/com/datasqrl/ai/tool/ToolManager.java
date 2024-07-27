package com.datasqrl.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

public interface ToolManager {

  Map<String, RuntimeFunctionDefinition> getFunctions();

  FunctionValidation<String> validateFunctionCall(String functionName, JsonNode arguments);

  String executeFunctionCall(String functionName, JsonNode arguments, @NonNull Map<String, Object> context) throws IOException;

  CompletableFuture<String> saveChatMessage(ChatMessageInterface message);

  <ChatMessage extends ChatMessageInterface> List<ChatMessage> getChatMessages(
      @NonNull Map<String, Object> context, int limit, @NonNull Class<ChatMessage> clazz);


}
