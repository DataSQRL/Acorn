package com.datasqrl.ai.trace;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class TraceUtil {

  public static RequestThrottler waitingRequestObserver(String modelProvider) {
    return switch (modelProvider) {
      case "groq" -> waitFor(30);
      default -> RequestThrottler.NONE;
    };
  }

  private static RequestThrottler waitFor(final int seconds) {
    return ctx -> {
      try {
        TimeUnit.SECONDS.sleep(seconds);
      } catch (InterruptedException e) {
        log.error("Could not to sleep {} seconds", seconds, e);
      }
    };
  }

}
