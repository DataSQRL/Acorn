package com.datasqrl.ai.models;

import java.util.List;
import java.util.Map;

public interface ChatClientProvider<Message> {

  List<Message> getChatHistory(Map<String, Object> context);

  Message chat(String message, Map<String, Object> context);
}
