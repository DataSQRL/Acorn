package com.datasqrl.ai.backend;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
public class ChatSession<Message> {

  private final FunctionBackend backend;
  private final Map<String, Object> sessionContext;
  private final String systemMessage;
  private final ModelBindings<Message> bindings;

  private final List<GenericChatMessage> messages = new ArrayList<>();

  public void addMessage(Message message) {
    GenericChatMessage convertedMsg = bindings.convertMessage(message, sessionContext);
    messages.add(convertedMsg);
    backend.saveChatMessage(convertedMsg);
  }

  public List<GenericChatMessage> retrieveMessageHistory(int limit) {
    if (!messages.isEmpty())
      throw new IllegalArgumentException("Can only retrieve message history at beginning of session");
    messages.addAll(backend.getChatMessages(sessionContext, limit, GenericChatMessage.class));
    return messages;
  }

  public List<Message> getChatHistory(boolean includeFunctionCalls) {
    List<GenericChatMessage> messages = retrieveMessageHistory(50);
    return messages.stream().map(bindings::convertMessage)
        .filter(message -> includeFunctionCalls || bindings.isUserOrAssistantMessage(message)).toList();
  }

  protected ContextWindow<GenericChatMessage> getWindow(int maxTokens, ModelAnalyzer<Message> analyzer) {
    GenericChatMessage systemMessage = bindings.createSystemMessage(this.systemMessage, sessionContext);
    final AtomicInteger numTokens = new AtomicInteger(0);
    numTokens.addAndGet(systemMessage.getNumTokens());
    ContextWindow.ContextWindowBuilder<GenericChatMessage> builder = ContextWindow.builder();
    backend.getFunctions().values().stream().map(f -> {
      numTokens.addAndGet(f.getNumTokens(analyzer));
      return f.getChatFunction();
    }).forEach(builder::function);
    if (numTokens.get() > maxTokens)
      throw new IllegalArgumentException("Function calls and system message too large for model: " + numTokens);
    int numMessages = messages.size();
    List<GenericChatMessage> resultMessages = new ArrayList<>();
    ListIterator<GenericChatMessage> listIterator = messages.listIterator(numMessages);
    while (listIterator.hasPrevious()) {
      GenericChatMessage message = listIterator.previous();
      numTokens.addAndGet(message.getNumTokens(msg -> analyzer.countTokens(bindings.convertMessage(msg))));
      if (numTokens.get() > maxTokens) break;
      resultMessages.add(message);
      numMessages--;
    }
    builder.message(systemMessage);
    Collections.reverse(resultMessages);
    builder.messages(resultMessages);
    ContextWindow<GenericChatMessage> window = builder.build();
    if (numMessages > 0) System.out.printf("Truncated the first %s messages\n", numMessages);
    return window;
  }

  public ChatSessionComponents<Message> getSessionComponents() {
    ContextWindow<GenericChatMessage> context = getWindow(bindings.getModelCompletionLength(), bindings.getTokenizer());
    return new ChatSessionComponents<>(context.getMessages().stream().map(bindings::convertMessage).toList(), context.getFunctions());
  }

}
