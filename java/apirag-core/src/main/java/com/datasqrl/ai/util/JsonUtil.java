package com.datasqrl.ai.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

public class JsonUtil {


  public static Optional<JsonNode> parseJson(String json) {
    ObjectMapper mapper = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    try {
      return Optional.of(mapper.readTree(json));
    } catch (Exception e) {
    }
    return Optional.empty();
  }

}
