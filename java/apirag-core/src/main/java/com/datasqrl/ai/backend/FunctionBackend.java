package com.datasqrl.ai.backend;

import com.datasqrl.ai.api.APIExecutor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.PathType;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An {@link FunctionBackend} defines and executes functions that a language model
 * can use. In addition, it provides methods to save and retrieve chat messages to give
 * the language model a "memory".
 *
 *
 */
@Value
public class FunctionBackend {

  public static final String SAVE_CHAT_FUNCTION_NAME = "_saveChatMessage";
  public static final String RETRIEVE_CHAT_FUNCTION_NAME = "_getChatMessages";

  private static Set<String> RESERVED_FUNCTION_NAMES = Set.of(SAVE_CHAT_FUNCTION_NAME.toLowerCase(),
      RETRIEVE_CHAT_FUNCTION_NAME.toLowerCase());

  Map<String, RuntimeFunctionDefinition> functions;

  Optional<RuntimeFunctionDefinition> saveChatFct;

  Optional<RuntimeFunctionDefinition> getChatsFct;

  APIExecutor apiExecutor;

  ObjectMapper mapper;


  /**
   * Constructs a {@link FunctionBackend} from the provided configuration file, {@link APIExecutor},
   * and {@link ModelAnalyzer}.
   *
   * The format of the configuration file is defined in the <a href="https://github.com/DataSQRL/apiRAG">Github repository</a>
   * and you can find examples underneath the {@code api-examples} directory.
   *
   * @param tools Json string that defines the tools
   * @param apiExecutor Executor for the API queries
   * @return An {@link FunctionBackend} instance
   * @throws IOException if configuration file cannot be read
   */
  public static FunctionBackend of(@NonNull String tools, @NonNull APIExecutor apiExecutor) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    List<RuntimeFunctionDefinition> functions = mapper.readValue(tools,
        new TypeReference<>() {
        });
    return new FunctionBackend(functions.stream()
        .filter(f -> !RESERVED_FUNCTION_NAMES.contains(f.getName().toLowerCase()))
        .collect(Collectors.toMap(RuntimeFunctionDefinition::getName, Function.identity())),
        functions.stream().filter(f -> f.getName().equalsIgnoreCase(SAVE_CHAT_FUNCTION_NAME)).findFirst(),
        functions.stream().filter(f -> f.getName().equalsIgnoreCase(RETRIEVE_CHAT_FUNCTION_NAME)).findFirst(),
        apiExecutor, mapper);
  }

  /**
   * Adds the given function to the backend
   * @param function
   */
  public void addFunction(RuntimeFunctionDefinition function) {
    functions.put(function.getName(), function);
  }


  /**
   * Saves the {@link GenericChatMessage} with the configured context asynchronously (i.e. does not block)
   *
   * @param message chat message to save
   * @return A future for this asynchronous operation which returns the result as a string.
   */
  public CompletableFuture<String> saveChatMessage(ChatMessageInterface message) {
    if (saveChatFct.isEmpty()) return CompletableFuture.completedFuture("Message saving disabled");
    JsonNode payload = mapper.valueToTree(message);
    return apiExecutor.executeWrite(saveChatFct.get().getApi().getQuery(), payload);
  }

  /**
   * Retrieves saved chat messages from the API via the configured function call.
   * If no function call for message retrieval is configured, an empty list is returned.
   *
   * Uses the configured context to retrieve user or context specific chat messages.
   *
   * @param context Arbitrary session context that identifies a user or provides contextual information.
   * @return Saved messages for the provided context
   */
  public <ChatMessage extends ChatMessageInterface> List<ChatMessage> getChatMessages(
      @NonNull Map<String, Object> context, int limit, @NonNull Class<ChatMessage> clazz) {
    if (getChatsFct.isEmpty()) return List.of();
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put("limit", limit);
    JsonNode variables = addOrOverrideContext(arguments, getChatsFct.get(), context);
    String graphqlQuery = getChatsFct.get().getApi().getQuery();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    try {
      String response = apiExecutor.executeQuery(graphqlQuery, variables);
      JsonNode root = mapper.readTree(response);
      JsonNode messages = root.path("data").path("messages");

      List<ChatMessage> chatMessages = new ArrayList<>();
      for (JsonNode node : messages) {
        ChatMessage chatMessage = mapper.treeToValue(node, clazz);
        chatMessages.add(chatMessage);
      }
      Collections.reverse(chatMessages); //newest should be last
      return chatMessages;
    } catch (IOException e) {
      e.printStackTrace();
      return List.of();
    }
  }

  /**
   * Validates a call to the function identified by name with the provided arguments.
   * Validates that the function exists and that the provided arguments are valid.
   *
   * @param functionName Name of the function to call
   * @param arguments Arguments to the function
   * @return
   */
  @SneakyThrows
  public FunctionValidation<String> validateFunctionCall(String functionName, JsonNode arguments) {
    RuntimeFunctionDefinition function = functions.get(functionName);
    String error = null;
    if (function == null) error = "Not a valid function name: " + functionName;
    else {
      //TODO: throw exception if json schema is not matched
      SchemaValidatorsConfig config = new SchemaValidatorsConfig();
      config.setPathType(PathType.JSON_POINTER);
      ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      FunctionDefinition def = function.getChatFunction();
      String schemaText = mapper.writeValueAsString(def.getParameters());
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
      JsonSchema schema = factory.getSchema(schemaText, config);
      Set<ValidationMessage> errors = schema.validate(arguments);
      if (!errors.isEmpty()) {
        error = "Invalid Schema: " + String.join("; ", errors.stream().map(ValidationMessage::toString).collect(Collectors.toList()));
      }
    }
    return new FunctionValidation<>(error == null, function != null && function.getType().isPassThrough(), error);
  }


  /**
   * Executes the given function with the provided arguments and context.
   *
   * @param functionName Name of the function to call
   * @param arguments Arguments to the function
   * @param context session context that is added to the arguments
   * @return The result as string
   * @throws IOException
   */
  public String executeFunctionCall(String functionName, JsonNode arguments, @NonNull Map<String, Object> context) throws IOException {
    RuntimeFunctionDefinition function = functions.get(functionName);
    if (function == null) throw new IllegalArgumentException("Not a valid function name: " + functionName);
    if (function.getType().isPassThrough())
      throw new IllegalArgumentException("Cannot execute passthrough functions: " + functionName);

    JsonNode variables = addOrOverrideContext(arguments, function, context);

    return switch (function.getType()) {
      case local -> function.getExecutable().apply(variables).toString();
      case graphql, rest -> {
        String graphqlQuery = function.getApi().getQuery();
        yield apiExecutor.executeQuery(graphqlQuery, variables);
      }
      default ->
          throw new IllegalArgumentException("Cannot execute function [" + functionName + "] of type: " + function.getType());
    };
  }

  private JsonNode addOrOverrideContext(JsonNode arguments, RuntimeFunctionDefinition function, @NonNull Map<String, Object> context) {
    // Create a copy of the original JsonNode to add context
    ObjectNode copyJsonNode;
    if (arguments == null || arguments.isEmpty()) {
      copyJsonNode = mapper.createObjectNode();
    } else {
      copyJsonNode = (ObjectNode) arguments.deepCopy();
    }
    // Add context
    for (String contextField : function.getContext()) {
      Object value = context.get(contextField);
      if (value == null) throw new IllegalArgumentException("Missing context field: " + contextField);
      copyJsonNode.putPOJO(contextField, value);
    }
    return copyJsonNode;
  }


}
