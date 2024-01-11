package com.datasqrl.ai;

import com.datasqrl.ai.backend.APIChatBackend;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Value;

/**
 * A simple streaming chatbot for the command line.
 * The implementation uses OpenAI's GPT3.5 with a default configuration
 * and {@link APIChatBackend} to call APIs that pull in requested data
 * as well as save and restore chat messages across sessions.
 *
 * This implementation is based on <a href="https://github.com/TheoKanning/openai-java/blob/main/example/src/main/java/example/OpenAiApiFunctionsWithStreamExample.java">https://github.com/TheoKanning/openai-java</a>
 * and meant only for demonstration and testing.
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
    service = new OpenAiService(openAIKey);
    this.backend = backend;
  }

  /**
   * Starts the chatbot on the command line which will accepts questions and produce responses.
   * Type "exit" to terminate.
   *
   * @param instructionMessage The system instruction message for the ChatBot
   */
  public void start(String instructionMessage) {
    Scanner scanner = new Scanner(System.in);
    ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), instructionMessage);
    MessageTruncator messageTruncator = new MessageTruncator(chatModel.getMaxInputTokens(), systemMessage,
        Encodings.newDefaultEncodingRegistry().getEncodingForModel(chatModel.getEncodingModel()));
    messages.addAll(backend.getChatMessages());


    System.out.print("First Query: ");
    ChatMessage firstMsg = new ChatMessage(ChatMessageRole.USER.value(), scanner.nextLine());
    messages.add(firstMsg);
    backend.saveChatMessage(firstMsg);

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
      backend.saveChatMessage(chatMessage);

      if (chatMessage.getFunctionCall() != null) {
        ChatFunctionCall fctCall = chatMessage.getFunctionCall();
        //System.out.println("Trying to execute " + fctCall.getName() + " with arguments " + fctCall.getArguments().toPrettyString());
        ChatMessage functionResponse = backend.executeAndConvertToMessageHandlingExceptions(fctCall);
        //System.out.println("Executed " + fctCall.getName() + ".");
        messages.add(functionResponse);
        backend.saveChatMessage(functionResponse);
        continue;
      }

      System.out.print("Next Query: ");
      String nextLine = scanner.nextLine();
      if (nextLine.equalsIgnoreCase("exit")) {
        System.exit(0);
      }
      ChatMessage nextMsg = new ChatMessage(ChatMessageRole.USER.value(), nextLine);
      messages.add(nextMsg);
      backend.saveChatMessage(nextMsg);
    }
  }

}