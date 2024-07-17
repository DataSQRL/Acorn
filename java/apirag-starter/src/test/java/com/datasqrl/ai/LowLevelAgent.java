package com.datasqrl.ai;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIExecutorFactory;
import com.datasqrl.ai.api.RESTExecutorFactory;
import com.datasqrl.ai.models.ChatSession;
import com.datasqrl.ai.models.ContextWindow;
import com.datasqrl.ai.models.openai.OpenAIModelBindings;
import com.datasqrl.ai.models.openai.OpenAIModelConfiguration;
import com.datasqrl.ai.tool.FunctionValidation;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.FunctionMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.MapConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple streaming chatbot for the command line.
 */
@Slf4j
@Value
public class LowLevelAgent {

  String systemPrompt;
  OpenAiService service;
  ToolsBackend toolsBackend;
  OpenAIModelConfiguration chatConfig = new OpenAIModelConfiguration(
      new MapConfiguration(Map.of(
          "name", "gpt-3.5-turbo",
          "temperature", 0.3)));

  /**
   * Initializes a command line chat bot using OpenAI
   *
   * @param toolsPath     A path to a tools file in JSON format
   * @param apiExecutor   An APIExecutor for the tools
   * @param systemPrompt  The system message for the LLM
   * @param openAIKey     The OpenAI API key to call the API
   */

  public LowLevelAgent(Path toolsPath, APIExecutor apiExecutor, String systemPrompt, String openAIKey) throws IOException {
    this.systemPrompt = systemPrompt;
    this.service = new OpenAiService(openAIKey, Duration.ofSeconds(60));
    List<RuntimeFunctionDefinition> tools = ToolsBackendFactory.readTools(toolsPath);
    this.toolsBackend = ToolsBackendFactory.of(tools, Map.of(APIExecutorFactory.DEFAULT_NAME, apiExecutor));
  }

  /**
   * Starts the chatbot on the command line which will accepts questions and produce responses.
   * Type "exit" to terminate.
   */
  public void start(Map<String, Object> context) throws IOException {
    Scanner scanner = new Scanner(System.in);
    OpenAIModelBindings modelBindings = new OpenAIModelBindings(chatConfig);
    ChatSession<ChatMessage, ChatFunctionCall> session = new ChatSession<>(toolsBackend, context, systemPrompt, modelBindings);

    System.out.print("First Query: ");
    ChatMessage firstMsg = new UserMessage(scanner.nextLine());
    session.addMessage(firstMsg);

    while (true) {
      ContextWindow<ChatMessage> contextWindow = session.getContextWindow();
      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
          .builder()
          .model(chatConfig.getModelName())
          .messages(contextWindow.getMessages())
          .functions(contextWindow.getFunctions())
          .functionCall("auto")
          .n(1)
          .topP(chatConfig.getTopP())
          .temperature(chatConfig.getTemperature())
          .maxTokens(chatConfig.getMaxOutputTokens())
          .logitBias(new HashMap<>())
          .build();
      Flowable<ChatCompletionChunk> flowable = service.streamChatCompletion(chatCompletionRequest);

      AtomicBoolean isFirst = new AtomicBoolean(true);
      AssistantMessage responseMessage = service.mapStreamToAccumulator(flowable)
          .doOnNext(accumulator -> {
            if (accumulator.isFunctionCall()) {
              if (isFirst.getAndSet(false)) {
                System.out.println("Executing function " + accumulator.getAccumulatedChatFunctionCall().getName() + "...");
              }
            } else {
              if (isFirst.getAndSet(false)) {
                System.out.print("Response: ");
              }
              if (accumulator.getMessageChunk().getContent() != null) {
                System.out.print(accumulator.getMessageChunk().getContent());
              }
            }
          })
          .doOnComplete(System.out::println)
          .lastElement()
          .blockingGet()
          .getAccumulatedMessage();
      session.addMessage(responseMessage);

      ChatFunctionCall functionCall = responseMessage.getFunctionCall();
      if (functionCall != null) {
        FunctionValidation<ChatMessage> functionValidation = session.validateFunctionCall(functionCall);
        if (functionValidation.isValid()) {
          log.info("Executing {} with arguments {}", functionCall.getName(), functionCall.getArguments().toPrettyString());
          ChatMessage functionResponse = session.executeFunctionCall(functionCall, context);
          log.info("Executed {} with results: {}", functionCall.getName(), functionResponse.getTextContent());
          session.addMessage(functionResponse);
        } else {
          log.info("Function call {} failed.", functionCall.getName());
          session.addMessage(new FunctionMessage("{\"error\": \"" + functionValidation.validationError().errorMessage() + "\"}", "error"));
        }
      } else {
        System.out.print("Next Query: ");
        String nextLine = scanner.nextLine();
        if (nextLine.equalsIgnoreCase("exit")) {
          System.exit(0);
        }
        ChatMessage nextMsg = new UserMessage(nextLine);
        session.addMessage(nextMsg);
      }
    }
  }

  public static void main(String... args) throws Exception {
    String systemPrompt = "You are an incredibly enthusiastic and joyous life coach. You provide advice to people. You look up any and all advice you give via the provided functions.";
    Path toolsPath = Path.of("java", "apirag-starter", "src", "main", "resources", "tools", "advice.tools.json");
    String openaiKey = System.getenv("OPENAI_API_KEY");
    APIExecutor apiExecutor = new RESTExecutorFactory().create(new MapConfiguration(Map.of(
        "type", "rest",
        "url", "https://api.adviceslip.com"
    )), "adviceapi");

    LowLevelAgent agent = new LowLevelAgent(toolsPath, apiExecutor, systemPrompt, openaiKey);
    Map<String, Object> context = Map.of("userid", 1);
    agent.start(context);
  }
}