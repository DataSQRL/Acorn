package com.datasqrl.ai.backend;

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
  public void printMetrics() {
    log.info("Meter LatencyTimer");
    log.info("Total count: {}", latencyTimer.count());
    log.info("Total amount: {}", latencyTimer.totalTime(TimeUnit.MILLISECONDS));
    log.info("Mean: {}", latencyTimer.mean(TimeUnit.MILLISECONDS));
    log.info("Max: {}", latencyTimer.max(TimeUnit.MILLISECONDS));

    log.info("Meter InputTokenCounter");
    log.info("Total count: {}", inputTokenCounter.count());
    log.info("Total amount: {}", inputTokenCounter.totalAmount());
    log.info("Mean: {}", inputTokenCounter.mean());
    log.info("Max: {}", inputTokenCounter.max());

    log.info("Meter OutputTokenCounter");
    log.info("Total count: {}", outputTokenCounter.count());
    log.info("Total amount: {}", outputTokenCounter.totalAmount());
    log.info("Mean: {}", outputTokenCounter.mean());
    log.info("Max: {}", outputTokenCounter.max());

    log.info("Meter FunctionRetryCounter");
    log.info("Total count: {}", functionRetryCounter.count());
    log.info("Total amount: {}", functionRetryCounter.totalAmount());
    log.info("Mean: {}", functionRetryCounter.mean());
    log.info("Max: {}", functionRetryCounter.max());
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
