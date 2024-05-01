package com.datasqrl.ai.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class AbstractChatSession<Message, FunctionCall> {

  protected final FunctionBackend backend;
  protected final Map<String, Object> sessionContext;

  protected GenericChatMessage systemMessage = null;
  protected final List<GenericChatMessage> messages = new ArrayList<>();

  public void addMessage(Message message) {
    GenericChatMessage convertedMsg = convertMessage(message);
    messages.add(convertedMsg);
    backend.saveChatMessage(convertedMsg);
  }

  public List<Message> retrieveMessageHistory(int limit) {
    if (!messages.isEmpty()) throw new IllegalArgumentException("Can only retrieve message history at beginning of session");
    messages.addAll(backend.getChatMessages(sessionContext, limit, GenericChatMessage.class));
    return messages.stream().map(this::convertMessage).collect(Collectors.toUnmodifiableList());
  }

  protected ContextWindow<GenericChatMessage> getWindow(int maxTokens, ModelAnalyzer<Message> analyzer) {
    final AtomicInteger numTokens = new AtomicInteger(0);
    numTokens.addAndGet(systemMessage.getNumTokens());
    ContextWindow.ContextWindowBuilder<GenericChatMessage> builder = ContextWindow.builder();
    backend.getFunctions().values().stream().map(f -> {
          numTokens.addAndGet(f.getNumTokens(analyzer));
          return f.getChatFunction();
        }).forEach(builder::function);
    if (numTokens.get()>maxTokens) throw new IllegalArgumentException("Function calls and system message too large for model: " + numTokens);
    int numMessages = messages.size();
    List<GenericChatMessage> resultMessages = new ArrayList<>();
    ListIterator<GenericChatMessage> listIterator = messages.listIterator(numMessages);
    while (listIterator.hasPrevious()) {
      GenericChatMessage message = listIterator.previous();
      numTokens.addAndGet(message.getNumTokens(msg -> analyzer.countTokens(convertMessage(msg))));
      if (numTokens.get()>maxTokens) break;
      resultMessages.add(message);
      numMessages--;
    }
    builder.message(systemMessage);
    Collections.reverse(resultMessages);
    builder.messages(resultMessages);
    ContextWindow<GenericChatMessage> window = builder.build();
    if (numMessages>0) System.out.printf("Truncated the first %s messages\n", numMessages);
    return window;
  }

  public abstract FunctionValidation<Message> validateFunctionCall(FunctionCall call);

  public abstract Message executeFunctionCall(FunctionCall call);


  protected abstract Message convertMessage(GenericChatMessage message);

  protected abstract GenericChatMessage convertMessage(Message message);




}
