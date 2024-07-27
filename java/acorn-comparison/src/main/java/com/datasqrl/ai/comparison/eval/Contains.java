package com.datasqrl.ai.comparison.eval;

import com.google.auto.service.AutoService;
import java.util.Arrays;
import lombok.Value;
import org.apache.commons.configuration2.Configuration;

@Value
public class Contains implements Evaluation {

  public static final String DEFAULT_DELIMITER = ",";

  String delimiter;

  @Override
  public boolean evaluate(String expected, String provided) {
    String[] expectedElements = Arrays.stream(expected.split(delimiter)).map(String::trim)
        .map(String::toLowerCase).toArray(String[]::new);
    provided = provided.toLowerCase();
    for (String expectedElement : expectedElements) {
      if (provided.contains(expectedElement.trim())) return true;
    }
    return false;
  }

  @AutoService(EvaluationFactory.class)
  public static class Factory implements EvaluationFactory {

    @Override
    public String getType() {
      return "contains";
    }

    @Override
    public Evaluation create(Configuration settings) {
      return new Contains(settings.getString("delimiter", DEFAULT_DELIMITER));
    }
  }
}
