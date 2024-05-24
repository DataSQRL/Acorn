package com.datasqrl.ai.backend;

import java.util.Map;

public interface ChatMessageInterface {

  public String getContent();
  public Map<String,Object> getContext();


}
