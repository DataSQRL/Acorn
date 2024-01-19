package com.datasqrl.ai.spring;

import com.datasqrl.ai.Examples;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.APIChatBackend;
import com.datasqrl.ai.backend.AnnotatedChatMessage;
import com.datasqrl.ai.backend.MessageTruncator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest.ChatCompletionRequestFunctionCall;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class SimpleServer {

  public static final String DEFAULT_GRAPHQL_ENDPOINT = "http://localhost:8888/graphql";

  public static void main(String[] args) {
    SpringApplication.run(SimpleServer.class, args);
  }

  @CrossOrigin(origins = "*")
  @RestController
  public static class MessageController {

    private final Examples example;
    OpenAiService service;
    GraphQLExecutor apiExecutor;
    APIChatBackend backend;
    ChatMessage systemMessage;
    MessageTruncator messageTruncator;
    List functions;

    String chartFunctionName="";


    public MessageController(@Value("${example:nutshop}") String exampleName) throws IOException {
      this.example = Examples.valueOf(exampleName.trim().toUpperCase());
      String openAIToken = System.getenv("OPENAI_TOKEN");
      this.service = new OpenAiService(openAIToken, Duration.ofSeconds(60));
      String graphQLEndpoint = DEFAULT_GRAPHQL_ENDPOINT;
      this.apiExecutor = new GraphQLExecutor(graphQLEndpoint);
      this.backend = APIChatBackend.of(Path.of(example.getConfigFile()), apiExecutor);
      this.systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), example.getSystemPrompt());
      this.messageTruncator = new MessageTruncator(example.getModel().getMaxInputTokens(), systemMessage,
          Encodings.newDefaultEncodingRegistry().getEncodingForModel(example.getModel().getEncodingModel()));
      this.functions = new ArrayList<>();
      this.functions.addAll(backend.getChatFunctions());
      if (example.isSupportCharts()) {
        ObjectMapper objectMapper = new ObjectMapper();
        URL url = SimpleServer.class.getClassLoader().getResource("plotfunction.json");
        if (url != null) {
          try {
            JsonNode functionJson = objectMapper.readTree(url);
            this.chartFunctionName = functionJson.get("name").textValue();
            this.functions.add(functionJson);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }

    private Map<String,Object> getContext(String userId) {
      return Map.of(example.getUserIdFieldName(), example.getPrepareUserIdFct().apply(
          userId));
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Map<String,Object> context = getContext(userId);
      List<AnnotatedChatMessage> messages = backend.getChatMessages(context, 50);
      return messages.stream().filter(msg -> {
        ChatMessage m = msg.getMessage();
        ChatMessageRole role = ChatMessageRole.valueOf(m.getRole().toUpperCase());
        switch (role) {
          case USER:
          case ASSISTANT:
            return true;
        }
        return false;
      }).map(ResponseMessage::of).collect(Collectors.toUnmodifiableList());
    }

    @PostMapping("/messages")
    public ResponseMessage postMessage(@RequestBody InputMessage message) {
      Map<String,Object> context = getContext(message.getUserId());

      List<ChatMessage> messages = new ArrayList<>(30);
      backend.getChatMessages(context, 20).stream().map(AnnotatedChatMessage::getMessage)
              .forEach(messages::add);
      System.out.printf("Retrieved %d messages\n", messages.size());
      ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), message.getContent());
      messages.add(chatMessage);
      backend.saveChatMessage(chatMessage, context);


      while (true) {
        System.out.println("Calling OpenAI");
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
            .builder()
            .model(example.getModel().getOpenAIModel())
            .messages(messageTruncator.truncateMessages(messages, backend.getChatFunctions()))
            .functions(functions)
            .functionCall(ChatCompletionRequestFunctionCall.of("auto"))
            .n(1)
            .maxTokens(example.getModel().getCompletionLength())
            .logitBias(new HashMap<>())
            .build();
        ChatMessage responseMessage = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();
        messages.add(responseMessage); // don't forget to update the conversation with the latest response
        backend.saveChatMessage(responseMessage, context);

        ChatFunctionCall functionCall = responseMessage.getFunctionCall();
        if (functionCall != null && !functionCall.getName().equalsIgnoreCase(chartFunctionName)) {
          System.out.println("Executing " + functionCall.getName() + " with arguments " + functionCall.getArguments().toPrettyString());
          ChatMessage functionResponse = backend.executeAndConvertToMessageHandlingExceptions(
              functionCall, context);
          //System.out.println("Executed " + fctCall.getName() + ".");
          messages.add(functionResponse);
          backend.saveChatMessage(functionResponse, context);
        } else {
          //The text answer
          return ResponseMessage.of(responseMessage);
        }
      }
    }
  }
}
