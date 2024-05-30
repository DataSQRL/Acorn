package com.datasqrl.ai.models.groq;

import com.datasqrl.ai.backend.ChatSession;
import com.datasqrl.ai.backend.ChatSessionComponents;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.FunctionMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.theokanning.openai.service.OpenAiService.defaultClient;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

public class GroqChatProvider extends ChatClientProvider<ChatMessage, ChatFunctionCall> {

  private final GroqChatModel model;
  private final OpenAiService service;
  private final String systemPrompt;
  private ChatFunctionCall errorFunctionCall = null;
  public static final String GROQ_URL = "https://api.groq.com/openai/v1/";

  public GroqChatProvider(GroqChatModel model, FunctionBackend backend, String systemPrompt) {
    super(backend, new GroqModelBindings(model));
    this.model = model;
    this.systemPrompt = systemPrompt;
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

  @Override
  public ChatMessage chat(String message, Map<String, Object> context) {
    ChatSession<ChatMessage, ChatFunctionCall> session = new ChatSession<>(backend, context, systemPrompt, bindings);
    int numMsg = session.retrieveMessageHistory(20).size();
    System.out.printf("Retrieved %d messages\n", numMsg);
    ChatMessage chatMessage = new UserMessage(message);
    session.addMessage(chatMessage);

    while (true) {
      System.out.println("Calling GROQ with model " + model.getModelName());
      ChatSessionComponents<ChatMessage> sessionComponents = session.getSessionComponents();
      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
          .builder()
          .model(model.getModelName())
          .messages(sessionComponents.getMessages())
          .functions(sessionComponents.getFunctions())
          .functionCall("auto")
          .n(1)
          .temperature(0.2)
          .maxTokens(model.getCompletionLength())
          .logitBias(new HashMap<>())
          .build();
      AssistantMessage responseMessage;
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
          ChatFunctionCall functionCall = getFunctionCallFromText(responseText).orElse(null);
          responseMessage = new AssistantMessage("", "", null, functionCall);
          System.out.println("!!!Remapped content to function call");
        }
      }
      session.addMessage(responseMessage);
      ChatFunctionCall functionCall = responseMessage.getFunctionCall();
      if (functionCall != null) {
        FunctionValidation<ChatMessage> fctValid = this.validateFunctionCall(functionCall);
        if (fctValid.isValid()) {
          if (fctValid.isPassthrough()) { // return as is - evaluated on frontend
            return responseMessage;
          } else {
            System.out.println("Executing " + functionCall.getName() + " with arguments "
                + functionCall.getArguments().toPrettyString());
            FunctionMessage functionResponse = (FunctionMessage) this.executeFunctionCall(functionCall, context);
            System.out.println("Executed " + functionCall.getName() + " with results: " + functionResponse.getTextContent());
            session.addMessage(functionResponse);
          }
        } // TODO: add retry in case of invalid function call
      } else {
        // The text answer
        return responseMessage;
      }
    }
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
      if (code == 400 && body != null && body.contentType() != null && body.contentType().subtype() != null
          && body.contentType().subtype().toLowerCase().equals("json")) {
        BufferedSource source = body.source();
        source.request(Long.MAX_VALUE); // Buffer the entire body.
        Buffer buffer = source.buffer();
        Charset charset = body.contentType().charset(StandardCharsets.UTF_8);
        // Clone the existing buffer is they can only read once so we still want to pass
        // the original one to the chain.
        String jsonText = buffer.clone().readString(charset);
        errorFunctionCall = getFunctionCallFromGroqError(jsonText);
        if (errorFunctionCall != null) {
          System.out.println("!!!Extracted function call from 400");
        }
      }
      return response;
    }
  }

  @Override
  public FunctionMessage convertExceptionToMessage(String error) {
    return new FunctionMessage("{\"error\": \"" + error + "\"}", "error");
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

  public static Optional<ChatFunctionCall> getFunctionCallFromText(String text) {
    return JsonUtil.parseJson(text).map(json -> new ChatFunctionCall(json.get("function").asText(), json.get("parameters")));
  }
}
