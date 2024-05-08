package com.datasqrl.ai.models;

public interface ChatMessageFormatter<Message> {
  String encodeMessage(Message message);
  Message decodeMessage(String message, String roleHint);
}
