package com.datasqrl.ai.backend;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenericFunctionCall {

  String name;
  JsonNode arguments;


  @SneakyThrows
  @JsonValue
  public String toString() {
    return "{"
        + "\"name\": \"" + name + "\", "
        + "\"arguments\": " + new ObjectMapper().writeValueAsString(arguments)
        + "}";
  }
}
