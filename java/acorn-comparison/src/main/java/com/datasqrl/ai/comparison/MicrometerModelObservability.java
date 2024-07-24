package com.datasqrl.ai.comparison;

import com.datasqrl.ai.tool.ModelObservability;
import com.google.common.base.Stopwatch;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@Value
public class MicrometerModelObservability implements ModelObservability {

  public static final String APP_NAME_TAG = "app";

  MeterRegistry meterRegistry;
  Timer latencyTimer;
  DistributionSummary inputTokenCounter;
  DistributionSummary outputTokenCounter;
  DistributionSummary functionRetryCounter;

  public MicrometerModelObservability(MeterRegistry meterRegistry, String applicationName) {
    this.meterRegistry = meterRegistry;
    this.latencyTimer = Timer.builder("model.execution.time")
        .tags(APP_NAME_TAG, applicationName)
        .publishPercentileHistogram()
        .publishPercentiles(0.1, 0.5, 0.9)
        .register(meterRegistry);
    this.inputTokenCounter = DistributionSummary.builder("model.tokens.input")
        .tags(APP_NAME_TAG, applicationName)
        .register(meterRegistry);
    this.outputTokenCounter = DistributionSummary.builder("model.tokens.output")
        .tags(APP_NAME_TAG, applicationName)
        .register(meterRegistry);
    this.functionRetryCounter = DistributionSummary.builder("function.call.retries")
        .tags(APP_NAME_TAG, applicationName)
        .register(meterRegistry);
  }

  @Override
  public Trace start() {
    return new StandardTrace();
  }

  @Override
  public String exportToCSV() {
    StringBuilder s = new StringBuilder();
    s.append(latencyTimer.count()).append(", ");
    s.append(latencyTimer.totalTime(TimeUnit.MILLISECONDS)).append(", ");
    s.append(latencyTimer.mean(TimeUnit.MILLISECONDS)).append(", ");
//    for (ValueAtPercentile valueAtPercentile : latencyTimer.takeSnapshot().percentileValues()) {
//      s.append(valueAtPercentile.percentile()).append(":");
//      s.append(valueAtPercentile.value(TimeUnit.MILLISECONDS)).append(", ");
//    }
    s.append(latencyTimer.max(TimeUnit.MILLISECONDS)).append(", ");
    s.append(inputTokenCounter.totalAmount()).append(", ");
    s.append(inputTokenCounter.mean()).append(", ");
    s.append(outputTokenCounter.totalAmount()).append(", ");
    s.append(outputTokenCounter.mean()).append(", ");
    s.append(functionRetryCounter.totalAmount());
    return s.toString();
  }

  @Value
  @AllArgsConstructor
  public class StandardTrace implements Trace {

    Stopwatch timer = Stopwatch.createStarted();

    @Override
    public void stop() {
      timer.stop();
    }

    @Override
    public void complete(int numInputTokens, int numOutputTokens, boolean retry) {
      latencyTimer.record(timer.elapsed());
      inputTokenCounter.record(numInputTokens);
      outputTokenCounter.record(numOutputTokens);
      functionRetryCounter.record(retry ? 1 : 0);
    }
  }
}
