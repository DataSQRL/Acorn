package com.datasqrl.ai.models.google;

import com.datasqrl.ai.Examples;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionDefinition;
import com.datasqrl.ai.backend.FunctionType;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.models.bedrock.BedrockChatMessage;
import com.datasqrl.ai.models.bedrock.BedrockChatModel;
import com.datasqrl.ai.models.bedrock.BedrockChatProvider;
import com.datasqrl.ai.models.groq.GroqChatModel;
import com.datasqrl.ai.models.groq.GroqChatProvider;
import com.datasqrl.ai.models.openai.OpenAiChatModel;
import com.datasqrl.ai.models.openai.OpenAiChatProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.FunctionDeclaration;
import com.google.cloud.vertexai.api.FunctionResponse;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.generativeai.ChatSession;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class SimpleServer {

  public static void main(String[] args) {
    try {
      SpringApplication.run(SimpleServer.class, args);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @CrossOrigin(origins = "*")
  @RestController
  public static class MessageController {

    private final Examples example;
    private ChatClientProvider chatClientProvider;
    GraphQLExecutor apiExecutor;
    FunctionBackend backend;

    public static void traverseAndModify(JsonNode node) {
      if (node.isObject()) {
        ObjectNode objectNode = (ObjectNode) node;
        // Iterate over the fields of the object node
        objectNode.fieldNames().forEachRemaining((String fieldName) -> {
          JsonNode childNode = objectNode.get(fieldName);
          if ("type".equals(fieldName) && childNode.isTextual()) {
            String capsType = childNode.textValue().toUpperCase();
            objectNode.put(fieldName, capsType); // Increment age by 1
          } else {
            traverseAndModify(childNode); // Recursive call
          }
        });
      } else if (node.isArray()) {
        // If the node is an array, iterate over the elements
        for (JsonNode arrayElement : node) {
          traverseAndModify(arrayElement); // Recursive call
        }
      }
    }

    @SneakyThrows
    public MessageController(@Value("${example:nutshop}") String exampleName) {
      this.example = Examples.valueOf(exampleName.trim().toUpperCase());
      String graphQLEndpoint = example.getApiURL();
      this.apiExecutor = new GraphQLExecutor(graphQLEndpoint);
      this.backend = FunctionBackend.of(Path.of(example.getConfigFile()), apiExecutor);
      ObjectMapper objectMapper = new ObjectMapper();
      if (example.getPlotFunction().isPresent()) {
        URL url = SimpleServer.class.getClassLoader().getResource(example.getPlotFunction().getResourceFile());
        if (url != null) {
          try {
            FunctionDefinition plotFunction = objectMapper.readValue(url, FunctionDefinition.class);
            this.backend.addFunction(RuntimeFunctionDefinition.builder()
                .type(FunctionType.visualize)
                .function(plotFunction)
                .context(List.of())
                .build());
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

      String PROJECT_ID = "vertex-gemini-424313";
      String LOCATION = "europe-west4";
      String MODEL_NAME = "gemini-1.5-flash";
      String promptText = "What was my expense per category last week?";

      VertexAI vertexAi = new VertexAI(PROJECT_ID, LOCATION);
      Tool.Builder toolBuilder = Tool.newBuilder();
      this.backend.getFunctions().values().stream()
          .map(RuntimeFunctionDefinition::getChatFunction)
          .map(fromValue -> objectMapper.convertValue(fromValue, JsonNode.class))
          .map(f -> {
            System.out.println("Function def before: " + f);
            traverseAndModify(f);
            System.out.println("Function def after: " + f);
            try {
              String funDef = objectMapper.writeValueAsString(f);
              FunctionDeclaration.Builder builder = FunctionDeclaration.newBuilder();
              try {
                JsonFormat.parser().merge(funDef, builder);
                FunctionDeclaration functionDeclaration = builder.build();
                System.out.println("Built Function declaration:\n" + functionDeclaration);
                return functionDeclaration;
              } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
              }
            } catch (JsonProcessingException e) {
              throw new RuntimeException(e);
            }

          })
          .forEach(toolBuilder::addFunctionDeclarations);

      Tool tool = toolBuilder.build();
      System.out.println("Tool:\n" + tool);
      Content c = ContentMaker.forRole("user").fromString("");
      GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAi)
          .withSystemInstruction(ContentMaker.fromString(example.getSystemPrompt()))
          .withTools(Arrays.asList(tool));
      ChatSession chat = model.startChat();
      Content.newBuilder().addParts(Part.newBuilder().setFunctionResponse(FunctionResponse.newBuilder().mergeFrom().build())
      System.out.println(String.format("User Input: %s", promptText));
      GenerateContentResponse response = chat.sendMessage(promptText);

      System.out.println("\nPrint response: ");
      System.out.println(ResponseHandler.getContent(response));

      // Provide an answer to the model so that it knows what the result
      // of a "function call" is.
      Content content =
          ContentMaker.fromMultiModalData(
              PartMaker.fromFunctionResponse(
                  "getCurrentWeather",
                  Collections.singletonMap("currentWeather", "sunny")));
      System.out.println("Provide the function response: ");
      System.out.println(content);
      response = chat.sendMessage(content);

      // See what the model replies now
      System.out.println("Print response: ");
      String finalAnswer = ResponseHandler.getText(response);
      System.out.println(finalAnswer);


      this.chatClientProvider = switch (example.getProvider()) {
        case OPENAI -> new OpenAiChatProvider((OpenAiChatModel) example.getModel(), example.getSystemPrompt(), backend);
        case GROQ -> new GroqChatProvider((GroqChatModel) example.getModel(), example.getSystemPrompt(), backend);
        case BEDROCK ->
            new BedrockChatProvider((BedrockChatModel) example.getModel(), example.getSystemPrompt(), backend);
      }

      ;
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Map<String, Object> context = example.getContext(userId);
      return switch (example.getProvider()) {
        case OPENAI, GROQ ->
            chatClientProvider.getChatHistory(context).stream().map(msg -> ProviderMessageMapper.toResponse((ChatMessage) msg)).toList();
        case BEDROCK ->
            chatClientProvider.getChatHistory(context).stream().map(msg -> ProviderMessageMapper.toResponse((BedrockChatMessage) msg)).toList();
      };
    }

    @PostMapping("/messages")
    public ResponseMessage postMessage(@RequestBody InputMessage message) {
      System.out.println("\nUser #" + message.getUserId() + ": " + message.getContent());
      Map<String, Object> context = example.getContext(message.getUserId());
      return switch (example.getProvider()) {
        case OPENAI, GROQ ->
            ProviderMessageMapper.toResponse((ChatMessage) chatClientProvider.chat(message.getContent(), context));
        case BEDROCK ->
            ProviderMessageMapper.toResponse((BedrockChatMessage) chatClientProvider.chat(message.getContent(), context));
      };
    }
  }
}
