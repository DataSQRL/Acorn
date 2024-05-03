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
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.theokanning.openai.service.OpenAiService.defaultClient;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

@SpringBootApplication
public class SimpleServer {

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
      this.service = this.getService();
      String graphQLEndpoint = example.getApiURL();
      this.apiExecutor = new GraphQLExecutor(graphQLEndpoint);
      this.backend = FunctionBackend.of(Path.of(example.getConfigFile()), apiExecutor);
      this.systemMessage = new SystemMessage(example.getSystemPrompt());
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

    private OpenAiService getService() {
      String groqApiKey = System.getenv("GROQ_API_KEY");
      ObjectMapper mapper = defaultObjectMapper();
      HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
      logging.setLevel(HttpLoggingInterceptor.Level.BODY);
      OkHttpClient client = defaultClient(groqApiKey, Duration.ofSeconds(60))
              .newBuilder()
              .addInterceptor(logging)
              .build();
      Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.groq.com/openai/v1/") // Retrofit automatically cuts the 'openai/' part of this baseURL for the service requests :(
              .client(client)
              .addConverterFactory(JacksonConverterFactory.create(mapper))
              .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
              .build();
      OpenAiApi api = retrofit.create(OpenAiApi.class);
      OpenAiService service = new OpenAiService(api);
      return service;
    }

    private OpenAIChatSession getSession(Map<String,Object> context) {
      return new OpenAIChatSession(example.getModel(), systemMessage, backend, context);
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Map<String,Object> context = example.getContext(userId);
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
      Map<String,Object> context = example.getContext(message.getUserId());
      OpenAIChatSession session = getSession(context);
      int numMsg = session.retrieveMessageHistory(20).size();
      System.out.printf("Retrieved %d messages\n", numMsg);
      ChatMessage chatMessage = new UserMessage(message.getContent());
      session.addMessage(chatMessage);

      while (true) {
        System.out.println("Calling Groq with model " + example.getModel().name());
        ChatCompletionRequest chatCompletionRequest = session.setContext(ChatCompletionRequest
            .builder()
            .model(example.getModel().getOpenAIModel()))
            .functionCall("auto")
            .n(1)
            .maxTokens(example.getModel().getCompletionLength())
            .logitBias(new HashMap<>())
            .build();
        AssistantMessage responseMessage = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();
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
              FunctionMessage functionResponse = session.executeFunctionCall(functionCall);
              System.out.println("Executed " + functionCall.getName() + " with results: " + functionResponse.getTextContent());
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
