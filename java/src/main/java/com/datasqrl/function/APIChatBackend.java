package com.datasqrl.function;

import com.datasqrl.api.APIExecutor;
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
import lombok.Value;

@Value
public class APIChatBackend {

  public static final String SAVE_CHAT_FUNCTION_NAME = "_saveChatMessage";
  public static final String RETRIEVE_CHAT_FUNCTION_NAME = "_getChatMessages";

  private static Set<String> RESERVED_FUNCTION_NAMES = Set.of(SAVE_CHAT_FUNCTION_NAME.toLowerCase(),
      RETRIEVE_CHAT_FUNCTION_NAME.toLowerCase());

  Map<String, APIFunctionDefinition> functions;

  Optional<APIFunctionDefinition> saveChat;

  Optional<APIFunctionDefinition> getChats;

  Map<String, Object> context;

  APIExecutor apiExecutor;

  ObjectMapper mapper;


  public static APIChatBackend of(Path toolFile, APIExecutor apiExecutor, Map<String, Object> context) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    List<APIFunctionDefinition> functions = mapper.readValue(toolFile.toFile(),
        new TypeReference<List<APIFunctionDefinition>>(){});
    return new APIChatBackend(functions.stream()
        .filter(f -> !RESERVED_FUNCTION_NAMES.contains(f.getName().toLowerCase()))
        .collect(Collectors.toMap(APIFunctionDefinition::getName,
        Function.identity())),
        functions.stream().filter(f -> f.getName().equalsIgnoreCase(SAVE_CHAT_FUNCTION_NAME)).findFirst(),
        functions.stream().filter(f -> f.getName().equalsIgnoreCase(RETRIEVE_CHAT_FUNCTION_NAME)).findFirst(),
        context, apiExecutor, mapper);
  }

  public List<FunctionDefinition> getChatFunctions() {
    return functions.values().stream().map(f -> f.getChatFunction(context.keySet()))
        .collect(Collectors.toUnmodifiableList());
  }

  public ChatMessage executeAndConvertToMessageHandlingExceptions(ChatFunctionCall call) {
    try {
      return new ChatMessage(ChatMessageRole.FUNCTION.value(), execute(call), call.getName());
    } catch (Exception exception) {
      exception.printStackTrace();
      return convertExceptionToMessage(exception);
    }
  }

  public CompletableFuture<String> saveChatMessage(ChatMessage message) {
    if (saveChat.isEmpty()) return CompletableFuture.completedFuture("No message saving");
    ChatMessageWithContext msgWContext = ChatMessageWithContext.of(message,context);
    JsonNode payload = mapper.valueToTree(msgWContext);
//    System.out.println("Saved: " + payload);
    return apiExecutor.executeWrite(saveChat.get().getApi().getQuery(), payload);
  }

  public List<ChatMessage> getChatMessages() {
    if (getChats.isEmpty()) return List.of();
    JsonNode variables = addOrOverrideContext(null, getChats.get());
    String graphqlQuery = getChats.get().getApi().getQuery();
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

  @SuppressWarnings("unchecked")
  private String execute(ChatFunctionCall call) throws IOException {
    APIFunctionDefinition function = functions.get(call.getName());
    if (function == null) throw new IllegalArgumentException("Could not find function: " + call.getName());

    JsonNode variables = addOrOverrideContext(call.getArguments(), function);
    String graphqlQuery = function.getApi().getQuery();

    return apiExecutor.executeQuery(graphqlQuery, variables);
  }

  public JsonNode addOrOverrideContext(JsonNode arguments, APIFunctionDefinition function) {
    // Create a copy of the original JsonNode
    ObjectNode copyJsonNode;
    if (arguments==null || arguments.isEmpty()) {
      copyJsonNode = mapper.createObjectNode();
    } else {
      copyJsonNode = (ObjectNode) arguments.deepCopy();
    }
    // Add/override fields
    for (String contextField : function.getContext()) {
      Object value = context.get(contextField);
      if (value==null) throw new IllegalArgumentException("Missing context field: " + contextField);
      copyJsonNode.putPOJO(contextField, value);
    }
    return copyJsonNode;
  }

  public ChatMessage convertExceptionToMessage(Exception exception) {
    String error = exception.getMessage() == null ? exception.toString() : exception.getMessage();
    return new ChatMessage(ChatMessageRole.FUNCTION.value(), "{\"error\": \"" + error + "\"}", "error");
  }

}
