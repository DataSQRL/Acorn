package com.datasqrl.ai.backend;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenericFunctionCall {

  String name;
  JsonNode arguments;

}
