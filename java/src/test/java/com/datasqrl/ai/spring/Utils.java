package com.datasqrl.ai.spring;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatFunctionCall;

public class Utils {

  public static boolean isValidJson(String json) {
    ObjectMapper mapper = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    try {
      mapper.readTree(json);
    } catch (JacksonException e) {
      return false;
    }
    return true;
  }

  public static ChatFunctionCall getFunctionCallFromText(String text) {
    if (isValidJson(text)) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(text);
        return new ChatFunctionCall(json.get("function").asText(), json.get("parameters"));
      } catch (JsonProcessingException e) {
        System.out.println("Could not parse text function call:\n" + text);
        e.printStackTrace();
      }
    }
    return null;
  }
}
