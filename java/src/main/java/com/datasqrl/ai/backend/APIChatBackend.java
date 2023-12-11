package com.datasqrl.ai.backend;

import com.datasqrl.ai.api.APIExecutor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Value;

/**
 * An {@link APIChatBackend} defines and executes functions that a language model
 * can use. In addition, it provides methods to save and retrieve chat messages to give
 * the language model a "memory".
 *
 *
 */
@Value
public class APIChatBackend {

  public static final String SAVE_CHAT_FUNCTION_NAME = "_saveChatMessage";
  public static final String RETRIEVE_CHAT_FUNCTION_NAME = "_getChatMessages";

  private static Set<String> RESERVED_FUNCTION_NAMES = Set.of(SAVE_CHAT_FUNCTION_NAME.toLowerCase(),
      RETRIEVE_CHAT_FUNCTION_NAME.toLowerCase());

  Map<String, APIFunctionDefinition> functions;

  Optional<APIFunctionDefinition> saveChatFct;

  Optional<APIFunctionDefinition> getChatsFct;

  Map<String, Object> context;

  APIExecutor apiExecutor;

  ObjectMapper mapper;


  /**
   * Constructs a {@link APIChatBackend} from the provided configuration file, {@link APIExecutor},
   * and context.
   *
   * The format of the configuration file is defined in the <a href="https://github.com/DataSQRL/SmartRAG">Github repository</a>
   * and you can find examples underneath the {@code api-examples} directory.
   *
   * @param configFile Path to a configuration file
   * @param apiExecutor Executor for the API queries
   * @param context Arbitrary session context that identifies a user or provides contextual information.
   * @return An {@link APIChatBackend} instance
   * @throws IOException if configuration file cannot be read
   */
  public static APIChatBackend of(@NonNull Path configFile, @NonNull APIExecutor apiExecutor,
      @NonNull Map<String, Object> context) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    List<APIFunctionDefinition> functions = mapper.readValue(configFile.toFile(),
        new TypeReference<List<APIFunctionDefinition>>(){});
    return new APIChatBackend(functions.stream()
        .filter(f -> !RESERVED_FUNCTION_NAMES.contains(f.getName().toLowerCase()))
        .collect(Collectors.toMap(APIFunctionDefinition::getName, Function.identity())),
        functions.stream().filter(f -> f.getName().equalsIgnoreCase(SAVE_CHAT_FUNCTION_NAME)).findFirst(),
        functions.stream().filter(f -> f.getName().equalsIgnoreCase(RETRIEVE_CHAT_FUNCTION_NAME)).findFirst(),
        context, apiExecutor, mapper);
  }

  /**
   * Returns the available {@link FunctionDefinition} to be used by the language model.
   *
   * @return List of {@link FunctionDefinition} that can be passed to the language model.
   */
  public List<FunctionDefinition> getChatFunctions() {
    return functions.values().stream().map(APIFunctionDefinition::getChatFunction)
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Executes the provided {@link ChatFunctionCall}.
   *
   * @param call Function call to execute
   * @return The result of the function call as a string.
   */
  public ChatMessage executeAndConvertToMessageHandlingExceptions(ChatFunctionCall call) {
    try {
      return new ChatMessage(ChatMessageRole.FUNCTION.value(), execute(call), call.getName());
    } catch (Exception exception) {
      exception.printStackTrace();
      return convertExceptionToMessage(exception);
    }
  }

  /**
   * Saves the {@link ChatMessage} with the configured context asynchronously (i.e. does not block)
   *
   * @param message chat message to save
   * @return A future for this asynchronous operation which returns the result as a string.
   */
  public CompletableFuture<String> saveChatMessage(ChatMessage message) {
    if (saveChatFct.isEmpty()) return CompletableFuture.completedFuture("Message saving disabled");
    ChatMessageWithContext msgWContext = ChatMessageWithContext.of(message, context);
    JsonNode payload = mapper.valueToTree(msgWContext);
    return apiExecutor.executeWrite(saveChatFct.get().getApi().getQuery(), payload);
  }

  /**
   * Retrieves saved chat messages from the API via the configured function call.
   * If no function call for message retrieval is configured, an empty list is returned.
   *
   * Uses the configured context to retrieve user or context specific chat messages.
   *
   * @return Saved messages for the provided context
   */
  public List<ChatMessage> getChatMessages() {
    if (getChatsFct.isEmpty()) return List.of();
    JsonNode variables = addOrOverrideContext(null, getChatsFct.get());
    String graphqlQuery = getChatsFct.get().getApi().getQuery();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    try {
      String response = apiExecutor.executeQuery(graphqlQuery, variables);
      JsonNode root = mapper.readTree(response);
      JsonNode messages = root.path("data").path("messages");

      List<ChatMessage> chatMessages = new ArrayList<>();
      for (JsonNode node : messages) {
        ChatMessage chatMessage = mapper.treeToValue(node, ChatMessage.class);
        chatMessages.add(chatMessage);
      }
      return chatMessages;
    } catch (IOException e) {
      e.printStackTrace();
      return List.of();
    }
  }

  private String execute(ChatFunctionCall call) throws IOException {
    APIFunctionDefinition function = functions.get(call.getName());
    if (function == null) throw new IllegalArgumentException("Could not find function: " + call.getName());

    JsonNode variables = addOrOverrideContext(call.getArguments(), function);
    String graphqlQuery = function.getApi().getQuery();

    return apiExecutor.executeQuery(graphqlQuery, variables);
  }

  private JsonNode addOrOverrideContext(JsonNode arguments, APIFunctionDefinition function) {
    // Create a copy of the original JsonNode to add context
    ObjectNode copyJsonNode;
    if (arguments==null || arguments.isEmpty()) {
      copyJsonNode = mapper.createObjectNode();
    } else {
      copyJsonNode = (ObjectNode) arguments.deepCopy();
    }
    // Add context
    for (String contextField : function.getContext()) {
      Object value = context.get(contextField);
      if (value==null) throw new IllegalArgumentException("Missing context field: " + contextField);
      copyJsonNode.putPOJO(contextField, value);
    }
    return copyJsonNode;
  }

  private ChatMessage convertExceptionToMessage(Exception exception) {
    String error = exception.getMessage() == null ? exception.toString() : exception.getMessage();
    return new ChatMessage(ChatMessageRole.FUNCTION.value(), "{\"error\": \"" + error + "\"}", "error");
  }

}
