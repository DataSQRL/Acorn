package com.datasqrl.ai.spring;

import com.datasqrl.ai.Examples;
import com.datasqrl.ai.ModelProvider;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.*;
import com.datasqrl.ai.models.groq.GroqChatModel;
import com.datasqrl.ai.models.groq.GroqChatSession;
import com.datasqrl.ai.models.openai.OpenAiChatModel;
import com.datasqrl.ai.models.openai.OpenAIChatSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.completion.chat.FunctionMessage;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
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

    public static final String GROQ_URL = "https://api.groq.com/openai/v1/";
    private final Examples example;
    OpenAiService service;
    GraphQLExecutor apiExecutor;
    FunctionBackend backend;
    ChatMessage systemMessage;

    private String createLlama3SystemPrompt(String prompt, Collection<RuntimeFunctionDefinition> functions) {
      ObjectMapper objectMapper = new ObjectMapper();
      String functionText = functions.stream()
          .map(f ->
              objectMapper.createObjectNode()
                  .put("type", "function")
                  .set("function", objectMapper.valueToTree(f.getFunction()))
          )
          .map(value -> {
            try {
              return objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
              throw new RuntimeException(e);
            }
          })
          .collect(Collectors.joining("\n\n"));

      String text = "<|begin_of_text|>\n"
          + "<|start_header_id|>system<|end_header_id|>\n"
          + prompt + "\n"
          + functionText + "\n"
          + "<|eot_id|>\n";
      return text;
    }

    private String createLlama3UserMessage(String content) {
      return "<|start_header_id|>user<|end_header_id|>\n"
          + content + "\n"
          + "<|eot_id|>\n";
    }

    private String createLlama3AssistantMessage(String content) {
      return "<|start_header_id|>assistant<|end_header_id|>\n"
          + content + "\n"
          + "<|eot_id|>\n";
    }

    private JSONObject promptBedrock(BedrockRuntimeClient client, String modelId, String prompt) {
      JSONObject request = new JSONObject()
          .put("prompt", prompt)
          .put("max_gen_len", 512)
          .put("temperature", 0.1F)
          .put("top_p", 0.5F);

      System.out.println("Bedrock Request: " + request.toString());
      System.out.println("Prompt: " + prompt);
      InvokeModelRequest invokeModelRequest = InvokeModelRequest.builder()
          .modelId(modelId)
          .body(SdkBytes.fromUtf8String(request.toString()))
          .build();

      InvokeModelResponse invokeModelResponse = client.invokeModel(invokeModelRequest);
      return new JSONObject(invokeModelResponse.body().asUtf8String());
    }

    @SneakyThrows
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
      String modelId = "meta.llama3-70b-instruct-v1:0";
      String userMessage = "Plot my expenses in January";
      EnvironmentVariableCredentialsProvider credentialsProvider = EnvironmentVariableCredentialsProvider.create();
      try (BedrockRuntimeClient bedrockClient = BedrockRuntimeClient.builder()
          .region(Region.US_WEST_2)
          .credentialsProvider(credentialsProvider)
          .build()) {
        String prompt = createLlama3SystemPrompt(example.getSystemPrompt(), backend.getFunctions().values())
            + createLlama3UserMessage(userMessage);
//        String prompt = createLlama3SystemPrompt(example.getSystemPrompt(), backend.getFunctions().values())
//            + createLlama3UserMessage(userMessage)
//            + createLlama3AssistantMessage("{\"function\": \"transactions\", \"arguments\": {\"customerid\": 123456, \"fromTime\": \"2024-01-01T00:00:00-00:00\", \"toTime\": \"2024-01-31T23:59:59-00:00\"}}")
//            + createLlama3UserMessage(userMessage);
        JSONObject responseAsJson = promptBedrock(bedrockClient, modelId, prompt);
        System.out.println("🤖Bedrock Response: ");
        System.out.println(responseAsJson);
        System.out.println(responseAsJson.get("generation"));
        String textContent = responseAsJson.get("generation").toString();
        if (textContent.contains("{\"function\":")) {
          int start = textContent.indexOf("{\"function\":");
          int end = textContent.lastIndexOf("{");
          String jsonContent = textContent.substring(start, end);
          System.out.println("Function call detected: " + jsonContent);
        } else {
          System.out.println("No Function call detected.");

        }
      } catch (Exception e) {
        System.out.println("AWS Bedrock Exception:\n" + e);
      }
    }

    //    Think of extracting this into a Utils class
    private OpenAiService getService() {
      return switch (this.example.getProvider()) {
        case OPENAI -> {
          String openAIToken = System.getenv("OPENAI_TOKEN");
          yield new OpenAiService(openAIToken, Duration.ofSeconds(60));
        }
        case GROQ -> {
          String groqApiKey = System.getenv("GROQ_API_KEY");
          ObjectMapper mapper = defaultObjectMapper();
          HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
          logging.setLevel(HttpLoggingInterceptor.Level.NONE); // Change to .BODY to see the request body
          OkHttpClient client = defaultClient(groqApiKey, Duration.ofSeconds(60))
              .newBuilder()
              .addInterceptor(logging)
              .build();
          Retrofit retrofit = new Retrofit.Builder().baseUrl(GROQ_URL)
              .client(client)
              .addConverterFactory(JacksonConverterFactory.create(mapper))
              .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
              .build();
          yield new OpenAiService(retrofit.create(OpenAiApi.class));
        }
        case BEDROCK -> null;
      };
    }

    private AbstractChatSession<ChatMessage, ChatFunctionCall> getSession(Map<String, Object> context) {
      if (example.getProvider() == ModelProvider.OPENAI) {
        return new OpenAIChatSession((OpenAiChatModel) example.getModel(), systemMessage, backend, context);
      } else {
        return new GroqChatSession((GroqChatModel) example.getModel(), systemMessage, backend, context);
      }
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Map<String, Object> context = example.getContext(userId);
      AbstractChatSession<ChatMessage, ChatFunctionCall> session = getSession(context);
      List<ChatMessage> messages = session.retrieveMessageHistory(50);
      return messages.stream().filter(m -> {
        ChatMessageRole role = ChatMessageRole.valueOf(m.getRole().toUpperCase());
        return switch (role) {
          case USER, ASSISTANT -> true;
          default -> false;
        };
      }).map(ResponseMessage::of).collect(Collectors.toUnmodifiableList());
    }

    @PostMapping("/messages")
    public ResponseMessage postMessage(@RequestBody InputMessage message) {
      Map<String, Object> context = example.getContext(message.getUserId());
      AbstractChatSession<ChatMessage, ChatFunctionCall> session = getSession(context);
      int numMsg = session.retrieveMessageHistory(20).size();
      System.out.printf("Retrieved %d messages\n", numMsg);
      ChatMessage chatMessage = new UserMessage(message.getContent());
      session.addMessage(chatMessage);

      while (true) {
        System.out.println("Calling " + example.getProvider() + " with model " + example.getModel().getModelName());
        ChatSessionComponents<ChatMessage> sessionComponents = session.getSessionComponents();
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
            .builder()
            .model(example.getModel().getModelName())
            .messages(sessionComponents.getMessages())
            .functions(sessionComponents.getFunctions())
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
              FunctionMessage functionResponse = (FunctionMessage) session.executeFunctionCall(functionCall);
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
