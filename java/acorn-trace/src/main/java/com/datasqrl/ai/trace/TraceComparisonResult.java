package com.datasqrl.ai.trace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TraceEquality.class, name = "TraceEquality"),
    @JsonSubTypes.Type(value = QualitativeTraceJudge.QualitativeResult.class, name = "QualitativeResult"),
})

public interface TraceComparisonResult {

  boolean isCorrect();

  @JsonIgnore
  default String getMessage() {
    return "";
  }

  default void assertCorrect() {
    if (!isCorrect()) throw new AssertionError(getMessage());
  }

  public static TraceComparisonResult combine(Iterable<TraceComparisonResult> results) {
    boolean isCorrect = true;
    StringBuilder message = new StringBuilder();
    for (TraceComparisonResult result : results) {
      if (!result.isCorrect()) {
        isCorrect = false;
        message.append(result.getMessage()).append("\n");
      }
    }
    final boolean isCorrectResult = isCorrect;
    return new TraceComparisonResult() {
      @Override
      public boolean isCorrect() {
        return isCorrectResult;
      }

      @Override
      public String getMessage() {
        return message.toString();
      }
    };
  }

}
