package com.datasqrl.ai.trace;

import com.datasqrl.ai.trace.QualitativeTraceJudge.QualitativeResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TraceJudgeTest {

  public static final Map<String,Object> JUDGE_MODEL_CONFIG = Map.of(
      "provider", "openai",
      "name", "gpt-4o-mini",
      "temperature", 0.2,
      "max_output_tokens", 512
  );

  @Test
  @Disabled("requires openai api key")
  @SneakyThrows
  public void evaluateTrace() {
    Trace finance = Trace.loadFromURL(TraceJudgeTest.class.getResource("/traces/trace-finance.json"));
    Trace reference = Trace.loadFromURL(TraceJudgeTest.class.getResource("/traces/trace-finance-reference.json"));
    Assertions.assertEquals(18, finance.size());

    QualitativeTraceJudge judge = QualitativeTraceJudge.fromConfiguration(new MapConfiguration(JUDGE_MODEL_CONFIG));
    judge.judgeAllResponses(reference, finance).forEach(QualitativeResult::assertCorrect);
    QualitativeResult result = judge.judge(reference.getFunctionCall(3,1), finance.getFunctionCall(3,1));
    System.out.println(result);
    result.assertCorrect();
  }


}
