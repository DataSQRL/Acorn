package com.datasqrl.ai.backend;

import java.util.Map;

public interface ChatMessageInterface {

  String getContent();
  Map<String,Object> getContext();


}
