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
      System.out.println("Message has function call");
      int start = textContent.indexOf("{\"function\":");
      int end = textContent.lastIndexOf("}") + 1;
      String jsonContent = textContent.substring(start, end);
      System.out.println("JsonString:\n" + jsonContent);
      if (!jsonContent.isEmpty()) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonContent);
        if (jsonNode != null) {
          functionCall = new BedrockFunctionCall(jsonNode.findValue("function").toString(), jsonNode.findValue("parameters"));
        }
      }
    }
    System.out.println(functionCall);
    return functionCall;
  }
}
