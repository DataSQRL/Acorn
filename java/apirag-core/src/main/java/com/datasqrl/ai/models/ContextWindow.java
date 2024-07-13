package com.datasqrl.ai.models;

import com.datasqrl.ai.tool.FunctionDefinition;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ContextWindow<Message> {

  @Singular
  List<Message> messages;
  @Singular
  List<FunctionDefinition> functions;
  int numTokens;

}
