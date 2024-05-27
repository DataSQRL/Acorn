package com.datasqrl.ai.backend;

import java.util.Map;

public interface ModelBindings<Message> {
  Message convertMessage(GenericChatMessage message);
  GenericChatMessage convertMessage(Message message, Map<String, Object> sessionContext);
  boolean isUserOrAssistantMessage(Message message);
  ModelAnalyzer<Message> getTokenizer();
  int getModelCompletionLength();
  GenericChatMessage createSystemMessage(String systemMessage, Map<String, Object> sessionContext);
}
