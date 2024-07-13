package com.datasqrl.ai.tool;

import com.datasqrl.ai.api.APIExecutor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public class ToolsBackendFactory {

  public static final String SAVE_CHAT_FUNCTION_NAME = "InternalSaveChatMessage";
  public static final String RETRIEVE_CHAT_FUNCTION_NAME = "InternalGetChatMessages";

  private static final ObjectMapper mapper = new ObjectMapper();

  public static List<RuntimeFunctionDefinition> readTools(@NonNull URL uri) throws IOException {
    String content = Resources.toString(uri, StandardCharsets.UTF_8);
    return readTools(content);
  }

  public static List<RuntimeFunctionDefinition> readTools(@NonNull Path path) throws IOException {
    String tools = Files.readString(path);
    return readTools(tools);
  }

  public static List<RuntimeFunctionDefinition> readTools(@NonNull String tools) throws IOException {
    return mapper.readValue(tools,
        new TypeReference<>() {
        });
  }

  public static ToolsBackend of(@NonNull List<RuntimeFunctionDefinition> functions, @NonNull Map<String,APIExecutor> apiExecutors) {
    ToolsBackend backend = new ToolsBackend(apiExecutors, mapper);
    for (RuntimeFunctionDefinition function : functions) {
      if (function.getName().equalsIgnoreCase(SAVE_CHAT_FUNCTION_NAME)) {
        backend.setSaveChatFct(function);
      } else if (function.getName().equalsIgnoreCase(RETRIEVE_CHAT_FUNCTION_NAME)) {
        backend.setGetChatsFct(function);
      } else {
        backend.addFunction(function);
      }
    }
    return backend;
  }
}
