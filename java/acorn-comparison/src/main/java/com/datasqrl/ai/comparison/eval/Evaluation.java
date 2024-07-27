package com.datasqrl.ai.comparison.eval;

import com.fasterxml.jackson.databind.JsonNode;

public interface Evaluation {

  boolean evaluate(String expected, String provided);

  default boolean evaluate(JsonNode expected, JsonNode provided) {
    if (expected==null ^ provided==null) return false;
    if (expected==null) return true;
    if (provided.isEmpty() ^ expected.isEmpty()) return false;
    return evaluate(expected.asText(), provided.asText());
  }


}
