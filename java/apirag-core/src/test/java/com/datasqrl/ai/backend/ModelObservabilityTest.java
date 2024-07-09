package com.datasqrl.ai.backend;

import com.datasqrl.ai.backend.ModelObservability.Trace;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModelObservabilityTest {

  private MicrometerModelObservability modelObservability;
  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  public void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    modelObservability = new MicrometerModelObservability(meterRegistry, "testApp");
  }

  @Test
  public void testTrace() throws InterruptedException {
    // Start a trace
    int number = 1000;
    for (int i = 1; i <= number; i++) {
      Trace trace = modelObservability.start();
      // Simulate some work
      Thread.sleep(5);
      // Stop the trace
      trace.stop();
      // Complete the trace with some token counts
      trace.complete(10*i, 20*i, i%2==0);
    }

    // Check the metrics
    assertEquals(number, modelObservability.getLatencyTimer().count());
    assertEquals(number, modelObservability.getFunctionRetryCounter().count());
    assertEquals(number/2, modelObservability.getFunctionRetryCounter().totalAmount());
    assertEquals(number, modelObservability.getInputTokenCounter().count());
    assertEquals(number, modelObservability.getOutputTokenCounter().count());
    assertEquals(number*(number+1)/2*10, modelObservability.getInputTokenCounter().totalAmount(), 0.001);
    assertEquals(number*(number+1)/2*20, modelObservability.getOutputTokenCounter().totalAmount(), 0.001);
    System.out.println(modelObservability.exportToCSV());
  }
}
