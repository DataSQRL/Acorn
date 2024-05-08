package com.datasqrl.ai.models;

public interface ChatMessageFormatter<Message> {
  public String formatMessage(Message message);
}
