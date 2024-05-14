package com.datasqrl.ai.spring;

import java.util.List;
import java.util.Map;

public interface ChatClientProvider {

  List<ResponseMessage> getChatHistory(Map<String, Object> context);

  ResponseMessage chat(InputMessage message, Map<String, Object> context);
}
