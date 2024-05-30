package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.backend.ChatSession;
import com.datasqrl.ai.backend.ChatSessionComponents;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.util.JsonUtil;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.FunctionMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.service.OpenAiService;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OpenAiChatProvider extends ChatClientProvider<ChatMessage, ChatFunctionCall> {

  private final OpenAiChatModel model;
  private final OpenAiService service;
  private final String systemPrompt;

  public OpenAiChatProvider(OpenAiChatModel model, FunctionBackend backend, String systemPrompt) {
    super(backend, new OpenAIModelBindings(model));
    this.model = model;
    this.systemPrompt = systemPrompt;
    String openAIToken = System.getenv("OPENAI_TOKEN");
    this.service = new OpenAiService(openAIToken, Duration.ofSeconds(60));
  }


  @Override
  public ChatMessage chat(String message, Map<String, Object> context) {
    ChatSession<ChatMessage, ChatFunctionCall> session = new ChatSession<>(backend, context, systemPrompt, bindings);
    int numMsg = session.retrieveMessageHistory(20).size();
    System.out.printf("Retrieved %d messages\n", numMsg);
    ChatMessage chatMessage = new UserMessage(message);
    session.addMessage(chatMessage);

    while (true) {
      System.out.println("Calling OpenAI with model " + model.getModelName());
      ChatSessionComponents<ChatMessage> sessionComponents = session.getSessionComponents();
      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
          .builder()
          .model(model.getModelName())
          .messages(sessionComponents.getMessages())
          .functions(sessionComponents.getFunctions())
          .functionCall("auto")
          .n(1)
          .temperature(0.2)
          .maxTokens(model.getContextWindowLength())
          .logitBias(new HashMap<>())
          .build();
      AssistantMessage responseMessage = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();
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
          if (fctValid.isPassthrough()) { //return as is - evaluated on frontend
            return responseMessage;
          } else {
            System.out.println("Executing " + functionCall.getName() + " with arguments "
                + functionCall.getArguments().toPrettyString());
            FunctionMessage functionResponse = (FunctionMessage) this.executeFunctionCall(functionCall, context);
            System.out.println("Executed " + functionCall.getName() + " with results: " + functionResponse.getTextContent());
            session.addMessage(functionResponse);
          }
        } //TODO: add retry in case of invalid function call
      } else {
        //The text answer
        return responseMessage;
      }
    }
  }

  @Override
  public FunctionMessage convertExceptionToMessage(String error) {
    return new FunctionMessage("{\"error\": \"" + error + "\"}", "error");
  }

  public static Optional<ChatFunctionCall> getFunctionCallFromText(String text) {
    return JsonUtil.parseJson(text).map(json -> new ChatFunctionCall(json.get("function").asText(), json.get("parameters")));
  }

}
