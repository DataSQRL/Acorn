package com.datasqrl.ai.backend;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIQuery;
import com.datasqrl.ai.util.ErrorHandling;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * An {@link FunctionBackend} defines and executes functions that a language model
 * can use. In addition, it provides methods to save and retrieve chat messages to give
 * the language model a "memory".
 *
 *
 */
@Slf4j
@Value
public class FunctionBackend {

  Map<String, RuntimeFunctionDefinition> functions;

  Optional<RuntimeFunctionDefinition> saveChatFct;

  Optional<RuntimeFunctionDefinition> getChatsFct;

  Map<String,APIExecutor> apiExecutors;

  ObjectMapper mapper;


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
    APIQuery query = saveChatFct.get().getApi();
    return getExecutor(query).executeQueryAsync(query, payload);
  }

  private APIExecutor getExecutor(APIQuery query) {
    ErrorHandling.checkArgument(apiExecutors.containsKey(query.getNameOrDefault()), "Could not find executor for API: %s", query.getNameOrDefault());
    return apiExecutors.get(query.getNameOrDefault());
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
    APIQuery query = getChatsFct.get().getApi();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    try {
      String response = getExecutor(query).executeQuery(query, variables);
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
    FunctionValidation.ValidationError<String> error = null;
    if (function == null) {
      error = new FunctionValidation.ValidationError<>("Not a valid function name: " + functionName,
          FunctionValidation.ValidationError.Type.FUNCTION_NOT_FOUND);
    } else {
      ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      FunctionDefinition def = function.getChatFunction();
      String schemaText = mapper.writeValueAsString(def.getParameters());
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
      JsonSchema schema = factory.getSchema(schemaText);
      Set<ValidationMessage> schemaErrors = schema.validate(arguments);
      if (!schemaErrors.isEmpty()) {
        String schemaErrorsText = schemaErrors.stream().map(ValidationMessage::toString).collect(Collectors.joining("; "));
        log.info("Function call had schema errors: {}", schemaErrorsText);
        error = new FunctionValidation.ValidationError<>("Invalid Schema: " + schemaErrorsText, FunctionValidation.ValidationError.Type.INVALID_JSON);
      }
    }
    return new FunctionValidation<>(error == null, function != null && function.getType().isClientExecuted(), error);
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
    if (function.getType().isClientExecuted())
      throw new IllegalArgumentException("Cannot execute client-side functions: " + functionName);

    JsonNode variables = addOrOverrideContext(arguments, function, context);
    APIQuery query = function.getApi();
    return switch (function.getType()) {
      case local -> function.getExecutable().apply(variables).toString();
      case api -> {
        yield getExecutor(query).executeQuery(query, variables);
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
