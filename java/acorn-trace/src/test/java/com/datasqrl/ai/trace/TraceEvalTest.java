package com.datasqrl.ai.trace;

import com.datasqrl.ai.trace.Trace.FunctionCall;
import org.junit.jupiter.api.Assertions;
import static com.datasqrl.ai.trace.TraceAssertions.*;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class TraceEvalTest {

  @Test
  @SneakyThrows
  public void evaluateTrace() {
    Trace finance = Trace.loadFromURL(TraceEvalTest.class.getResource("/traces/trace-finance.json"));
    Trace reference = Trace.loadFromURL(TraceEvalTest.class.getResource("/traces/trace-finance-reference.json"));
    Assertions.assertEquals(18, finance.size());
    assertContainsIgnoreCase(finance.getResponse(0).content(), "transaction", "spending");
    assertSameFunctionCalls(reference, finance, false);
  }

}
