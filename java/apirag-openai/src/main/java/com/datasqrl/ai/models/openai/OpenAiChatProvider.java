package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.backend.ChatSession;
import com.datasqrl.ai.backend.ContextWindow;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.backend.GenericChatMessage;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenAiChatProvider extends ChatClientProvider<ChatMessage, ChatFunctionCall> {

  private final OpenAiChatModel model;
  private final OpenAiService service;
  private final String systemPrompt;

  public OpenAiChatProvider(OpenAiChatModel model, FunctionBackend backend, String systemPrompt) {
    super(backend, new OpenAIModelBindings(model));
    this.model = model;
    this.systemPrompt = systemPrompt;
    String openAIToken = System.getenv("OPENAI_API_KEY");
    this.service = new OpenAiService(openAIToken, Duration.ofSeconds(60));
  }

  public GenericChatMessage chat(String message, Map<String, Object> context) {
    ChatSession<ChatMessage, ChatFunctionCall> session = new ChatSession<>(backend, context, systemPrompt, bindings);
    ChatMessage chatMessage = new UserMessage(message);
    session.addMessage(chatMessage);

    while (true) {
      log.debug("Calling OpenAI with model " + model.getModelName());
      ContextWindow<ChatMessage> contextWindow = session.getContextWindow();
      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
          .builder()
          .model(model.getModelName())
          .messages(contextWindow.getMessages())
          .functions(contextWindow.getFunctions())
          .functionCall("auto")
          .n(1)
          .temperature(0.2)
          .maxTokens(model.getCompletionLength())
          .logitBias(new HashMap<>())
          .build();
      AssistantMessage responseMessage = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();
      log.debug("Response:\n{}", responseMessage);
      String res = responseMessage.getTextContent();
      // Workaround for openai4j who doesn't recognize some function calls
      if (res != null) {
        String responseText = res.trim();
        if (responseText.startsWith("{\"function\"") && responseMessage.getFunctionCall() == null) {
          ChatFunctionCall functionCall = getFunctionCallFromText(responseText).orElse(null);
          responseMessage = new AssistantMessage("", functionCall.getName(), null, functionCall);
          log.debug("!!!Remapped content to function call");
        }
      }
      GenericChatMessage genericResponse = session.addMessage(responseMessage);
      ChatFunctionCall functionCall = responseMessage.getFunctionCall();
      if (functionCall != null) {
        FunctionValidation<ChatMessage> fctValid = session.validateFunctionCall(functionCall);
        if (fctValid.isValid()) {
          if (fctValid.isPassthrough()) { //return as is - evaluated on frontend
            return genericResponse;
          } else {
            log.debug("Executing {} with arguments {}", functionCall.getName(),
                functionCall.getArguments().toPrettyString());
            FunctionMessage functionResponse = (FunctionMessage) session.executeFunctionCall(functionCall, context);
            log.debug("Executed {} with results: {}" ,functionCall.getName(),functionResponse.getTextContent());
            session.addMessage(functionResponse);
          }
        } //TODO: add retry in case of invalid function call
      } else {
        //The text answer
        return genericResponse;
      }
    }
  }

  public static Optional<ChatFunctionCall> getFunctionCallFromText(String text) {
    return JsonUtil.parseJson(text).map(json -> new ChatFunctionCall(json.get("function").asText(), json.get("parameters")));
  }

}
