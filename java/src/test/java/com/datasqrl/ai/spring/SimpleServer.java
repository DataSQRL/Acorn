package com.datasqrl.ai.spring;

import com.datasqrl.ai.Examples;
import com.datasqrl.ai.ModelProvider;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.AbstractChatSession;
import com.datasqrl.ai.backend.ChatSessionComponents;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionDefinition;
import com.datasqrl.ai.backend.FunctionType;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.models.ChatMessageEncoder;
import com.datasqrl.ai.models.bedrock.BedrockChatMessage;
import com.datasqrl.ai.models.bedrock.BedrockChatModel;
import com.datasqrl.ai.models.bedrock.BedrockChatRole;
import com.datasqrl.ai.models.bedrock.BedrockChatSession;
import com.datasqrl.ai.models.bedrock.BedrockFunctionCall;
import com.datasqrl.ai.models.bedrock.Llama3MessageEncoder;
import com.datasqrl.ai.models.groq.GroqChatModel;
import com.datasqrl.ai.models.groq.GroqChatSession;
import com.datasqrl.ai.models.openai.OpenAIChatSession;
import com.datasqrl.ai.models.openai.OpenAiChatModel;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiHttpException;
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
import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.OllamaStreamHandler;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatMessageRole;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatRequestBuilder;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatRequestModel;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatResult;
import io.github.amithkoujalgi.ollama4j.core.utils.Options;
import io.github.amithkoujalgi.ollama4j.core.utils.OptionsBuilder;
import lombok.SneakyThrows;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import java.nio.charset.Charset;
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
    try {
      SpringApplication.run(SimpleServer.class, args);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @CrossOrigin(origins = "*")
  @RestController
  public static class MessageController {

    public static final String GROQ_URL = "https://api.groq.com/openai/v1/";
    private final Examples example;
    OpenAiService service;
    GraphQLExecutor apiExecutor;
    FunctionBackend backend;
    BedrockRuntimeClient client;
    ChatMessageEncoder encoder;

    ChatFunctionCall errorFunctionCall = null;

    private JSONObject promptBedrock(BedrockRuntimeClient client, String modelId, String prompt, int maxTokens) {
      JSONObject request = new JSONObject()
          .put("prompt", prompt)
          .put("max_gen_len", maxTokens)
          .put("temperature", 0F);
      InvokeModelRequest invokeModelRequest = InvokeModelRequest.builder()
          .modelId(modelId)
          .body(SdkBytes.fromUtf8String(request.toString()))
          .build();
      InvokeModelResponse invokeModelResponse = client.invokeModel(invokeModelRequest);
      JSONObject jsonObject = new JSONObject(invokeModelResponse.body().asUtf8String());
      System.out.println("🤖Bedrock Response:\n" + jsonObject);
      return jsonObject;
    }

    @SneakyThrows
    public MessageController(@Value("${example:nutshop}") String exampleName) throws IOException {
      this.example = Examples.valueOf(exampleName.trim().toUpperCase());

      String host = "http://localhost:11434/";
      String model = "llama3";
      OllamaAPI ollamaAPI = new OllamaAPI(host);
      ollamaAPI.setRequestTimeoutSeconds(600);
      ollamaAPI.setVerbose(true);
      OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(model);
      Options options =
          new OptionsBuilder()
              .setTemperature(0.2f)
              .setNumCtx(example.getModel().getCompletionLength())
              .setTopK(1)
              .setTopP(0.9F)
              .build();
      OllamaChatRequestModel requestModel = builder.withMessage(OllamaChatMessageRole.SYSTEM, example.getSystemPrompt())
          .withMessage(OllamaChatMessageRole.USER, "What can you help me with?")
          .withOptions(options)
          .build();
      System.out.println("Ollama request: " + requestModel.toString());
      OllamaStreamHandler streamHandler = System.out::println;
      OllamaChatResult chatResult = ollamaAPI.chat(requestModel);
//      OllamaChatResult chatResult = ollamaAPI.chat(requestModel, streamHandler);
      System.out.println("Ollama answer: " + chatResult.getHttpStatusCode() + " in seconds: " + chatResult.getResponseTime() + ":\n" + chatResult.getResponse());

      requestModel = builder.withMessage(OllamaChatMessageRole.USER, "And what is the second largest city?")
          .build();

      chatResult = ollamaAPI.chat(requestModel);
//      chatResult = ollamaAPI.chat(requestModel, streamHandler);

      System.out.println("Ollama request: " + requestModel.toString());
      System.out.println("Ollama answer: " + chatResult.getHttpStatusCode() + "in seconds: " + chatResult.getResponseTime() + ":\n" + chatResult.getResponse());

      this.setService();
      this.setFormatter();
      String graphQLEndpoint = example.getApiURL();
      this.apiExecutor = new GraphQLExecutor(graphQLEndpoint);
      this.backend = FunctionBackend.of(Path.of(example.getConfigFile()), apiExecutor);
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

    private void setService() {
      switch (this.example.getProvider()) {
        case OPENAI -> {
          String openAIToken = System.getenv("OPENAI_TOKEN");
          this.service = new OpenAiService(openAIToken, Duration.ofSeconds(60));
        }
        case GROQ -> {
          String groqApiKey = System.getenv("GROQ_API_KEY");
          ObjectMapper mapper = defaultObjectMapper();
          HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
          logging.setLevel(HttpLoggingInterceptor.Level.NONE); // Change to .BODY to see the request body
          OkHttpClient client = defaultClient(groqApiKey, Duration.ofSeconds(60))
              .newBuilder()
              .addInterceptor(logging)
              .addInterceptor(new MyInterceptor())
              .build();
          Retrofit retrofit = new Retrofit.Builder().baseUrl(GROQ_URL)
              .client(client)
              .addConverterFactory(JacksonConverterFactory.create(mapper))
              .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
              .build();
          this.service = new OpenAiService(retrofit.create(OpenAiApi.class));
        }
        case BEDROCK -> {
          EnvironmentVariableCredentialsProvider credentialsProvider = EnvironmentVariableCredentialsProvider.create();
          this.client = BedrockRuntimeClient.builder()
              .region(Region.US_WEST_2)
              .credentialsProvider(credentialsProvider)
              .build();
        }
      }
    }

    private void setFormatter() {
      this.encoder = switch (example.getProvider()) {
        case OPENAI, GROQ -> null;
        case BEDROCK -> {
          if (example.getModel() == BedrockChatModel.LLAMA3_70B || example.getModel() == BedrockChatModel.LLAMA3_7B) {
            yield new Llama3MessageEncoder();
          } else {
            yield null;
          }
        }
      };
    }

    private AbstractChatSession getSession(Map<String, Object> context) {
      return switch (example.getProvider()) {
        case OPENAI ->
            new OpenAIChatSession((OpenAiChatModel) example.getModel(), new SystemMessage(example.getSystemPrompt()), backend, context);
        case GROQ ->
            new GroqChatSession((GroqChatModel) example.getModel(), new SystemMessage(example.getSystemPrompt()), backend, context);
        case BEDROCK -> new BedrockChatSession((BedrockChatModel) example.getModel(),
            new BedrockChatMessage(BedrockChatRole.SYSTEM, example.getSystemPrompt(), ""), backend, context);
      };
    }

    public boolean isValidJson(String json) {
      ObjectMapper mapper = new ObjectMapper()
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
      try {
        mapper.readTree(json);
      } catch (JacksonException e) {
        return false;
      }
      return true;
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Map<String, Object> context = example.getContext(userId);
      if (example.getProvider() == ModelProvider.BEDROCK) {
        BedrockChatSession session = (BedrockChatSession) getSession(context);
        List<BedrockChatMessage> messages = session.retrieveMessageHistory(50);
        return messages.stream().filter(m -> switch (m.getRole()) {
          case USER, ASSISTANT -> true;
          default -> false;
        }).map(ResponseMessage::of).collect(Collectors.toUnmodifiableList());
      } else {
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
    }

    @PostMapping("/messages")
    public ResponseMessage postMessage(@RequestBody InputMessage message) {
      System.out.println("\nUser #" + message.getUserId() + ": " + message.getContent());
      if (example.getProvider() == ModelProvider.BEDROCK) {
        return this.respondToBedrockMessage(message);
      } else {
        return this.respondToOpenAiApiMessage(message);

      }
    }

    private ChatFunctionCall getFunctionCallFromGroqError(String errorText) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(errorText);
        JsonNode failedGeneration = json.get("error").get("failed_generation");
        String cleanText = failedGeneration.asText().replace("`", "");
        json = mapper.readTree(cleanText);
        JsonNode toolJson = json.get("tool_calls").get(0);
        return new ChatFunctionCall(toolJson.get("function").get("name").asText(), toolJson.get("parameters"));
      } catch (JsonProcessingException e) {
        System.out.println("Could not parse groq error:\n" + errorText);
        e.printStackTrace();
        return null;
      }
    }

    private ChatFunctionCall getFunctionCallFromText(String text) {
      if (isValidJson(text)) {
        try {
          ObjectMapper mapper = new ObjectMapper();
          JsonNode json = mapper.readTree(text);
          return new ChatFunctionCall(json.get("function").asText(), json.get("parameters"));
        } catch (JsonProcessingException e) {
          System.out.println("Could not parse text function call:\n" + text);
          e.printStackTrace();
        }
      }
      return null;
    }

    // Workaround for groq API bug that throws 400 on some function calls
    class MyInterceptor implements Interceptor {
      @NotNull
      @Override
      public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        ResponseBody body = response.body();
        int code = response.code();
        if (code == 400 && body != null && body.contentType() != null && body.contentType().subtype() != null && body.contentType().subtype().toLowerCase().equals("json")) {
          BufferedSource source = body.source();
          source.request(Long.MAX_VALUE); // Buffer the entire body.
          Buffer buffer = source.buffer();
          Charset charset = body.contentType().charset(Charset.forName("UTF-8"));
          // Clone the existing buffer is they can only read once so we still want to pass the original one to the chain.
          String jsonText = buffer.clone().readString(charset);
          errorFunctionCall = getFunctionCallFromGroqError(jsonText);
          if (errorFunctionCall != null) {
            System.out.println("!!!Extracted function call from 400");
          }
        }
        return response;
      }
    }

    @SneakyThrows
    private ResponseMessage respondToOpenAiApiMessage(InputMessage message) {
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
            .temperature(0.2)
            .maxTokens(example.getModel().getCompletionLength())
            .logitBias(new HashMap<>())
            .build();
        AssistantMessage responseMessage = new AssistantMessage();
        try {
          responseMessage = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();
        } catch (OpenAiHttpException e) {
          // Workaround for groq API bug that throws 400 on some function calls
          if (e.statusCode == 400 && errorFunctionCall != null) {
            responseMessage = new AssistantMessage("", "", null, errorFunctionCall);
            errorFunctionCall = null;
          } else {
            throw e;
          }
        }
        System.out.println("Response:\n" + responseMessage);
        String res = responseMessage.getTextContent();
        // Workaround for openai4j who doesn't recognize some function calls
        if (res != null) {
          String responseText = res.trim();
          if (responseText.startsWith("{\"function\"") && responseMessage.getFunctionCall() == null) {
            ChatFunctionCall functionCall = getFunctionCallFromText(responseText);
            responseMessage = new AssistantMessage("", "", null, functionCall);
            System.out.println("!!!Remapped content to function call");
          }
        }
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

    private ResponseMessage respondToBedrockMessage(InputMessage message) {
      Map<String, Object> context = example.getContext(message.getUserId());
      BedrockChatSession session = (BedrockChatSession) getSession(context);
      int numMsg = session.retrieveMessageHistory(20).size();
      System.out.printf("Retrieved %d messages\n", numMsg);
      BedrockChatMessage chatMessage = new BedrockChatMessage(BedrockChatRole.USER, message.getContent(), "");
      session.addMessage(chatMessage);

      while (true) {
        ChatSessionComponents<BedrockChatMessage> sessionComponents = session.getSessionComponents();
        String prompt = sessionComponents.getMessages().stream()
            .map(value -> {
              return this.encoder.encodeMessage(value);
            })
            .collect(Collectors.joining("\n"));
        System.out.println("Calling " + example.getProvider() + " with model " + example.getModel().getModelName());
        JSONObject responseAsJson = promptBedrock(client, example.getModel().getModelName(), prompt, example.getModel().getCompletionLength());
        BedrockChatMessage responseMessage = (BedrockChatMessage) encoder.decodeMessage(responseAsJson.get("generation").toString(), BedrockChatRole.ASSISTANT.getRole());
        session.addMessage(responseMessage);
        BedrockFunctionCall functionCall = responseMessage.getFunctionCall();
        if (functionCall != null) {
          FunctionValidation<BedrockChatMessage> fctValid = session.validateFunctionCall(functionCall);
          if (fctValid.isValid()) {
            if (fctValid.isPassthrough()) { //return as is - evaluated on frontend
              return ResponseMessage.of(responseMessage);
            } else {
              System.out.println("Executing " + functionCall.getFunctionName() + " with arguments "
                  + functionCall.getArguments().toPrettyString());
              BedrockChatMessage functionResponse = session.executeFunctionCall(functionCall);
              System.out.println("Executed " + functionCall.getFunctionName() + " with results: " + functionResponse.getTextContent());
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
