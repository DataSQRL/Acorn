package com.datasqrl.ai.spring;

import com.datasqrl.ai.Examples;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionDefinition;
import com.datasqrl.ai.backend.FunctionType;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.models.openai.OpenAIChatSession;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    FunctionBackend backend;
    ChatMessage systemMessage;



    public MessageController(@Value("${example:nutshop}") String exampleName) throws IOException {
      this.example = Examples.valueOf(exampleName.trim().toUpperCase());
      String openAIToken = System.getenv("OPENAI_TOKEN");
      this.service = new OpenAiService(openAIToken, Duration.ofSeconds(60));
      String graphQLEndpoint = DEFAULT_GRAPHQL_ENDPOINT;
      this.apiExecutor = new GraphQLExecutor(graphQLEndpoint);
      this.backend = FunctionBackend.of(Path.of(example.getConfigFile()), apiExecutor);
      this.systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), example.getSystemPrompt());
      if (example.isSupportCharts()) {
        ObjectMapper objectMapper = new ObjectMapper();
        URL url = SimpleServer.class.getClassLoader().getResource("plotfunction.json");
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
    }

    private Map<String,Object> getContext(String userId) {
      return Map.of(example.getUserIdFieldName(), example.getPrepareUserIdFct().apply(
          userId));
    }

    private OpenAIChatSession getSession(Map<String,Object> context) {
      return new OpenAIChatSession(example.getModel(), systemMessage, backend, context);
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Map<String,Object> context = getContext(userId);
      OpenAIChatSession session = getSession(context);
      List<ChatMessage> messages = session.retrieveMessageHistory(50);
      return messages.stream().filter(m -> {
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
      OpenAIChatSession session = getSession(context);
      int numMsg = session.retrieveMessageHistory(20).size();
      System.out.printf("Retrieved %d messages\n", numMsg);
      ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), message.getContent());
      session.addMessage(chatMessage);

      while (true) {
        System.out.println("Calling OpenAI");
        ChatCompletionRequest chatCompletionRequest = session.setContext(ChatCompletionRequest
            .builder()
            .model(example.getModel().getOpenAIModel()))
            .functionCall(ChatCompletionRequestFunctionCall.of("auto"))
            .n(1)
            .maxTokens(example.getModel().getCompletionLength())
            .logitBias(new HashMap<>())
            .build();
        ChatMessage responseMessage = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();
        session.addMessage(responseMessage);

        ChatFunctionCall functionCall = responseMessage.getFunctionCall();
        if (functionCall != null) {
          FunctionValidation<ChatMessage> fctValid = session.validateFunctionCall(functionCall);
          if (fctValid.isValid()) {
            if (fctValid.isPassthrough()) { //return as is - evaluated on frontend
              return ResponseMessage.of(responseMessage);
            } else {
              System.out.println("Executing " + functionCall.getName() + " with arguments "
                  + functionCall.getArguments().toPrettyString());
              ChatMessage functionResponse = session.executeFunctionCall(functionCall);
              //System.out.println("Executed " + fctCall.getName() + ".");
              session.addMessage(functionResponse);
            }
          } //TODO: add retry in case of invalid function call
        } else {
          //The text answer
          return ResponseMessage.of(responseMessage);
        }
      }
    }
  }
}
