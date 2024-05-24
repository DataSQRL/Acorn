package com.datasqrl.ai.models.bedrock;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
public class BedrockFunctionCall {
  String functionName;
  JsonNode arguments;
}
