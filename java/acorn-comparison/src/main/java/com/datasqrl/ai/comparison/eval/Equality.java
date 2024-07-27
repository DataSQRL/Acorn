package com.datasqrl.ai.comparison.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;
import org.apache.commons.configuration2.Configuration;

public class Equality implements Evaluation{

  public static final double DELTA = 0.000001;

  @Override
  public boolean evaluate(String expected, String provided) {
    return expected.equalsIgnoreCase(provided.trim());
  }

  @Override
  public boolean evaluate(JsonNode expected, JsonNode provided) {
    if (expected.isNumber()) {
      if (!provided.isNumber()) return false;
      return Math.abs(expected.asDouble() - provided.asDouble()) < DELTA;
    } else {
      return expected.equals(provided);
    }
  }

  @AutoService(EvaluationFactory.class)
  public static class Factory implements EvaluationFactory {

    @Override
    public String getType() {
      return "equality";
    }

    @Override
    public Evaluation create(Configuration settings) {
      return new Equality();
    }
  }

}
