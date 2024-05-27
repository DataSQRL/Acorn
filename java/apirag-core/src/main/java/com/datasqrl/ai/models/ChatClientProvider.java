package com.datasqrl.ai.models;

import java.util.Map;

public interface ChatClientProvider<Message> {

  Message chat(String message, Map<String, Object> context);
}
