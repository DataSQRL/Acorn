package com.datasqrl.ai.backend;

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
