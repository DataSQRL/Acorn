package com.datasqrl.ai;

import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.APIChatBackend;
import com.datasqrl.ai.backend.AnnotatedChatMessage;
import com.datasqrl.ai.backend.MessageTruncator;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.ModelType;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * A simple streaming chatbot for the command line.
 * The implementation uses OpenAI's GPT models with a default configuration
 * and {@link APIChatBackend} to call APIs that pull in requested data
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
  APIChatBackend backend;
  ChatModel chatModel = ChatModel.GPT35_TURBO;

  List<ChatMessage> messages = new ArrayList<>();

  /**
   * Initializes a command line chat bot
   *
   * @param openAIKey The OpenAI API key to call the API
   * @param backend An initialized backend to use for function execution and chat message persistence
   */
  public CmdLineChatBot(String openAIKey, APIChatBackend backend) {
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
    ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), instructionMessage);
    MessageTruncator messageTruncator = new MessageTruncator(chatModel.getMaxInputTokens(), systemMessage,
        Encodings.newDefaultEncodingRegistry().getEncodingForModel(chatModel.getEncodingModel()));
    messages.addAll(backend.getChatMessages(context, 30).stream().map(AnnotatedChatMessage::getMessage).collect(
        Collectors.toUnmodifiableList()));


    System.out.print("First Query: ");
    ChatMessage firstMsg = new ChatMessage(ChatMessageRole.USER.value(), scanner.nextLine());
    messages.add(firstMsg);
    backend.saveChatMessage(firstMsg, context);

    while (true) {
      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
          .builder()
          .model(chatModel.getOpenAIModel())
          .messages(messageTruncator.truncateMessages(messages, backend.getChatFunctions()))
          .functions(backend.getChatFunctions())
          .functionCall(ChatCompletionRequest.ChatCompletionRequestFunctionCall.of("auto"))
          .n(1)
          .maxTokens(chatModel.getCompletionLength())
          .logitBias(new HashMap<>())
          .build();
      Flowable<ChatCompletionChunk> flowable = service.streamChatCompletion(chatCompletionRequest);

      AtomicBoolean isFirst = new AtomicBoolean(true);
      ChatMessage chatMessage = service.mapStreamToAccumulator(flowable)
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
      messages.add(chatMessage); // don't forget to update the conversation with the latest response
      backend.saveChatMessage(chatMessage, context);

      if (chatMessage.getFunctionCall() != null) {
        ChatFunctionCall fctCall = chatMessage.getFunctionCall();
        //System.out.println("Trying to execute " + fctCall.getName() + " with arguments " + fctCall.getArguments().toPrettyString());
        ChatMessage functionResponse = backend.executeAndConvertToMessageHandlingExceptions(fctCall, context);
        //System.out.println("Executed " + fctCall.getName() + ".");
        messages.add(functionResponse);
        backend.saveChatMessage(functionResponse, context);
        continue;
      }

      System.out.print("Next Query: ");
      String nextLine = scanner.nextLine();
      if (nextLine.equalsIgnoreCase("exit")) {
        System.exit(0);
      }
      ChatMessage nextMsg = new ChatMessage(ChatMessageRole.USER.value(), nextLine);
      messages.add(nextMsg);
      backend.saveChatMessage(nextMsg, context);
    }
  }

  public static final String DEFAULT_GRAPHQL_ENDPOINT = "http://localhost:8888/graphql";

  public static void main(String... args) throws Exception {
    if (args==null || args.length==0) throw new IllegalArgumentException("Please provide the name of the example you want to run. One of: " + Arrays.toString(Examples.values()));
    Examples example = Examples.valueOf(args[0].trim().toUpperCase());
    String openAIToken = System.getenv("OPENAI_TOKEN");
    String graphQLEndpoint = DEFAULT_GRAPHQL_ENDPOINT;
    if (args.length>1) graphQLEndpoint = args[1];

    Map<String,Object> context = Map.of();
    if (example.hasUserId()) {
      Scanner scanner = new Scanner(System.in);
      System.out.print("Enter the User ID: ");
      String userid = scanner.nextLine();
      context = example.getContext(userid);
    }

    GraphQLExecutor apiExecutor = new GraphQLExecutor(graphQLEndpoint);
    APIChatBackend backend = APIChatBackend.of(Path.of(example.configFile), apiExecutor);
    CmdLineChatBot chatBot = new CmdLineChatBot(openAIToken, backend);
    chatBot.start(example.systemPrompt, context);
  }

}