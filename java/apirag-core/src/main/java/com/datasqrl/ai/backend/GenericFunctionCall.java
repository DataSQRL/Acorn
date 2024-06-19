package com.datasqrl.ai.backend;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
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

  @JsonCreator
  public static GenericFunctionCall create(String jsonString) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(jsonString);
    String name = jsonNode.get("name").asText();
    JsonNode arguments = jsonNode.get("arguments");
    return new GenericFunctionCall(name, arguments);
  }

  @SneakyThrows
  @JsonValue
  public String toString() {
    return "{"
        + "\"name\": \"" + name + "\", "
        + "\"arguments\": " + new ObjectMapper().writeValueAsString(arguments)
        + "}";
  }
}
