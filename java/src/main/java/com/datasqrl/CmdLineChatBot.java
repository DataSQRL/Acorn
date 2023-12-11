package com.datasqrl;

import com.datasqrl.api.GraphQLExecutor;
import com.datasqrl.function.APIChatBackend;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Value;

@Value
public class CmdLineChatBot {

  OpenAiService service;
  APIChatBackend backend;

  List<ChatMessage> messages = new ArrayList<>();

  public CmdLineChatBot(String openAIKey, APIChatBackend backend) {
    service = new OpenAiService(openAIKey);
    this.backend = backend;
  }

  public void start(String instructionMessage) throws Exception {
    Scanner scanner = new Scanner(System.in);
    ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), instructionMessage);
    messages.add(systemMessage);
    messages.addAll(backend.getChatMessages());


    System.out.print("First Query: ");
    ChatMessage firstMsg = new ChatMessage(ChatMessageRole.USER.value(), scanner.nextLine());
    messages.add(firstMsg);
    backend.saveChatMessage(firstMsg);

    while (true) {
      ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
          .builder()
          .model("gpt-3.5-turbo-0613")
          .messages(messages)
          .functions(backend.getChatFunctions())
          .functionCall(ChatCompletionRequest.ChatCompletionRequestFunctionCall.of("auto"))
          .n(1)
          .maxTokens(256)
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