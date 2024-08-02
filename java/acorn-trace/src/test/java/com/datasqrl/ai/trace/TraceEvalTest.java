package com.datasqrl.ai.trace;

import com.datasqrl.ai.trace.Trace.FunctionCall;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.datasqrl.ai.trace.TraceAssertions.assertContainsIgnoreCase;
import static com.datasqrl.ai.trace.TraceAssertions.assertSameFunctionCalls;

public class TraceEvalTest {


  @Test
  @SneakyThrows
  public void evaluateTrace() {
    Trace finance = Trace.loadFromURL(TraceEvalTest.class.getResource("/traces/trace-finance.json"));
    Trace reference = Trace.loadFromURL(TraceEvalTest.class.getResource("/traces/trace-finance-reference.json"));
    Assertions.assertEquals(18, finance.size());
    assertContainsIgnoreCase(finance.getResponse(0).content(), "transaction", "spending");
    assertSameFunctionCalls(reference.getAll(FunctionCall.class), finance, false);
    FunctionCall viz1 = finance.getFunctionCall(1,1);
    Assertions.assertEquals(Set.of(2601.59, 2730.05, 2328.52, 2344.55, 2218.76), Set.copyOf(ArgumentHelper.asDoubleList(viz1.arguments().get("values"))));
    Assertions.assertTrue(List.of("bar","column").contains(viz1.arguments().get("chartType").asText()));
  }

}
