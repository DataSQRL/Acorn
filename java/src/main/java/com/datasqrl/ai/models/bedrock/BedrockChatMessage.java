package com.datasqrl.ai.models.bedrock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.Value;

@Value
public class BedrockChatMessage {
  BedrockChatRole role;
  String textContent;
  String name;

  @SneakyThrows
  public BedrockFunctionCall getFunctionCall() {
    BedrockFunctionCall functionCall = null;
    if (textContent.contains("{\"function\":")) {
      int start = textContent.indexOf("{\"function\":");
      int end = textContent.lastIndexOf("{");
      String jsonContent = textContent.substring(start, end);
      if (!jsonContent.isEmpty()) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonContent);
        if (jsonNode != null) {
          System.out.println(jsonNode);
          functionCall = new BedrockFunctionCall(jsonNode.findValue("function").toString(), jsonNode.findValue("parameters"));
        }
      }
    }
    return functionCall;
  }
}
