package com.datasqrl.ai.models;

import java.util.List;
import java.util.Map;

public interface ChatClientProvider {

  List<ResponseMessage> getChatHistory(Map<String, Object> context);

  ResponseMessage chat(String message, Map<String, Object> context);
}
