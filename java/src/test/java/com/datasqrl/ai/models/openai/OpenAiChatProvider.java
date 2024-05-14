package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.backend.ChatSessionComponents;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.spring.ChatClientProvider;
import com.datasqrl.ai.spring.InputMessage;
import com.datasqrl.ai.spring.ResponseMessage;
import com.datasqrl.ai.spring.Utils;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenAiChatProvider implements ChatClientProvider {

  private final FunctionBackend backend;
  private final OpenAiService service;
  private final OpenAiChatModel model;
  private final SystemMessage systemPrompt;

  public OpenAiChatProvider(OpenAiChatModel model, String systemPrompt, FunctionBackend backend) {
    this.model = model;
    this.backend = backend;
    this.systemPrompt = new SystemMessage(systemPrompt);
    String openAIToken = System.getenv("OPENAI_TOKEN");
    this.service = new OpenAiService(openAIToken, Duration.ofSeconds(60));
  }

  @Override
  public List<ResponseMessage> getChatHistory(Map<String, Object> context) {
    OpenAIChatSession session = new OpenAIChatSession(model, systemPrompt, backend, context);
    List<ChatMessage> messages = session.retrieveMessageHistory(50);
    return messages.stream().filter(m -> {
      ChatMessageRole role = ChatMessageRole.valueOf(m.getRole().toUpperCase());
      return switch (role) {
        case USER, ASSISTANT -> true;
        default -> false;
      };
    }).map(ResponseMessage::of).collect(Collectors.toUnmodifiableList());
  }

  @Override
  public ResponseMessage chat(InputMessage message, Map<String, Object> context) {
    OpenAIChatSession session = new OpenAIChatSession(model, systemPrompt, backend, context);
    int numMsg = session.retrieveMessageHistory(20).size();
    System.out.printf("Retrieved %d messages\n", numMsg);
    ChatMessage chatMessage = new UserMessage(message.getContent());
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
          .maxTokens(model.getCompletionLength())
          .logitBias(new HashMap<>())
          .build();
      AssistantMessage responseMessage = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage();
      System.out.println("Response:\n" + responseMessage);
      String res = responseMessage.getTextContent();
      // Workaround for openai4j who doesn't recognize some function calls
      if (res != null) {
        String responseText = res.trim();
        if (responseText.startsWith("{\"function\"") && responseMessage.getFunctionCall() == null) {
          ChatFunctionCall functionCall = Utils.getFunctionCallFromText(responseText);
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


}
