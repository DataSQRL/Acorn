package com.datasqrl.ai.backend;

import com.datasqrl.ai.api.APIExecutor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;

public class FunctionBackendFactory {

  public static final String SAVE_CHAT_FUNCTION_NAME = "_saveChatMessage";
  public static final String RETRIEVE_CHAT_FUNCTION_NAME = "_getChatMessages";
  public static Set<String> RESERVED_FUNCTION_NAMES = Set.of(SAVE_CHAT_FUNCTION_NAME.toLowerCase(),
      RETRIEVE_CHAT_FUNCTION_NAME.toLowerCase());

  /**
   * Constructs a {@link FunctionBackend} from the provided configuration file, {@link APIExecutor},
   * and {@link ModelAnalyzer}.
   *
   * The format of the configuration file is defined in the <a href="https://github.com/DataSQRL/apiRAG">Github repository</a>
   * and you can find examples underneath the {@code api-examples} directory.
   *
   * @param tools Json string that defines the tools
   * @param apiExecutors Executors for the API queries by name
   * @return An {@link FunctionBackend} instance
   * @throws IOException if configuration file cannot be read
   */
  public static FunctionBackend of(@NonNull String tools, @NonNull Map<String,APIExecutor> apiExecutors) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    List<RuntimeFunctionDefinition> functions = mapper.readValue(tools,
        new TypeReference<>() {
        });
    return new FunctionBackend(functions.stream()
        .filter(f -> !RESERVED_FUNCTION_NAMES.contains(f.getName().toLowerCase()))
        .collect(Collectors.toMap(RuntimeFunctionDefinition::getName, Function.identity())),
        functions.stream().filter(f -> f.getName().equalsIgnoreCase(
            SAVE_CHAT_FUNCTION_NAME)).findFirst(),
        functions.stream().filter(f -> f.getName().equalsIgnoreCase(
            RETRIEVE_CHAT_FUNCTION_NAME)).findFirst(),
        apiExecutors, mapper);
  }
}
