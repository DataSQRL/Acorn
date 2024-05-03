package com.datasqrl.ai;

import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionValidation;
import com.datasqrl.ai.models.openai.ChatModel;
import com.datasqrl.ai.models.openai.OpenAIChatSession;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Value;

/**
 * A simple streaming chatbot for the command line.
 * The implementation uses OpenAI's GPT models with a default configuration
 * and {@link FunctionBackend} to call APIs that pull in requested data
 * as well as save and restore chat messages across sessions.
 *
 * This implementation is based on <a href="https://github.com/TheoKanning/openai-java/blob/main/example/src/main/java/example/OpenAiApiFunctionsWithStreamExample.java">https://github.com/TheoKanning/openai-java</a>
 * and meant only for demonstration and testing.
 *
 * To run the main method, you need to set your OPENAI token as an environment variable.
 * The main method expects the name of an {@link Examples} value.
 */
@Value
public class CmdLineChatBot {

  OpenAiService service;
  FunctionBackend backend;
  ChatModel chatModel = ChatModel.GPT35_TURBO;

  /**
   * Initializes a command line chat bot
   *
   * @param openAIKey The OpenAI API key to call the API
   * @param backend An initialized backend to use for function execution and chat message persistence
   */
  public CmdLineChatBot(String openAIKey, FunctionBackend backend) {
    service = new OpenAiService(openAIKey, Duration.ofSeconds(60));
    this.backend = backend;
  }

  /**
   * Starts the chatbot on the command line which will accepts questions and produce responses.
   * Type "exit" to terminate.
   *
   * @param instructionMessage The system instruction message for the ChatBot
   */
  public void start(String instructionMessage, Map<String, Object> context) {
    Scanner scanner = new Scanner(System.in);
    ChatMessage systemMessage = new SystemMessage(instructionMessage);
    OpenAIChatSession session = new OpenAIChatSession(chatModel, systemMessage, backend, context);


    System.out.print("First Query: ");
    ChatMessage firstMsg = new UserMessage(scanner.nextLine());
    session.addMessage(firstMsg);

    while (true) {
      ChatCompletionRequest chatCompletionRequest = session.setContext(ChatCompletionRequest
          .builder()
          .model(chatModel.getOpenAIModel()))
          .functionCall("auto")
          .n(1)
          .maxTokens(chatModel.getCompletionLength())
          .logitBias(new HashMap<>())
          .build();
      Flowable<ChatCompletionChunk> flowable = service.streamChatCompletion(chatCompletionRequest);

      AtomicBoolean isFirst = new AtomicBoolean(true);
      AssistantMessage chatMessage = service.mapStreamToAccumulator(flowable)
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
      session.addMessage(chatMessage);

      if (chatMessage.getFunctionCall() != null) {
        ChatFunctionCall fctCall = chatMessage.getFunctionCall();
        FunctionValidation<ChatMessage> fctValid = session.validateFunctionCall(fctCall);
        if (fctValid.isValid()) {
          System.out.println("Trying to execute " + fctCall.getName() + " with arguments " + fctCall.getArguments().toPrettyString());
          ChatMessage functionResponse = session.executeFunctionCall(fctCall);
          System.out.println("Executed " + fctCall.getName() + " with response: " + functionResponse.getTextContent());
          session.addMessage(functionResponse);
        } else {
          session.addMessage(fctValid.getErrorMessage());
        }
        continue;
      }

      System.out.print("Next Query: ");
      String nextLine = scanner.nextLine();
      if (nextLine.equalsIgnoreCase("exit")) {
        System.exit(0);
      }
      ChatMessage nextMsg = new UserMessage(nextLine);
      session.addMessage(nextMsg);
    }
  }

  public static void main(String... args) throws Exception {
    if (args==null || args.length==0) throw new IllegalArgumentException("Please provide the name of the example you want to run. One of: " + Arrays.toString(Examples.values()));
    Examples example = Examples.valueOf(args[0].trim().toUpperCase());
    String openAIToken = System.getenv("OPENAI_TOKEN");
    String graphQLEndpoint = example.getApiURL();
    if (args.length>1) graphQLEndpoint = args[1];

    Map<String,Object> context = Map.of();
    if (example.hasUserId()) {
      Scanner scanner = new Scanner(System.in);
      System.out.print("Enter the User ID: ");
      String userid = scanner.nextLine();
      context = example.getContext(userid);
    }

    GraphQLExecutor apiExecutor = new GraphQLExecutor(graphQLEndpoint);
    FunctionBackend backend = FunctionBackend.of(Path.of(example.configFile), apiExecutor);
    CmdLineChatBot chatBot = new CmdLineChatBot(openAIToken, backend);
    chatBot.start(example.systemPrompt, context);
  }

}