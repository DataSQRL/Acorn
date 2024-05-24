package com.datasqrl.ai.models;

public interface ChatMessageEncoder<Message> {
  String encodeMessage(Message message);
  Message decodeMessage(String message, String roleHint);
}
