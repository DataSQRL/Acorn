package com.datasqrl.function;

import com.theokanning.openai.completion.chat.ChatFunctionDynamic;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class APIFunctionDefinition {

  private String type;
  private FunctionDefinition function;
  private List<String> context;
  private APIFunctionQuery api;

  public String getName() {
    return function.getName();
  }

  public FunctionDefinition getChatFunction(Set<String> context) {
    return function.removeContext(context);
  }


}
