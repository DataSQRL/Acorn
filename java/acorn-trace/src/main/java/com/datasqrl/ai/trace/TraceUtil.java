package com.datasqrl.ai.trace;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class TraceUtil {

  public static void waitBetweenRequests(String modelProvider) {
    int seconds = switch (modelProvider) {
      case "groq" -> 30;
      default -> 0;
    };

  }

  public static RequestObserver waitingRequestObserver(String modelProvider) {
    return switch (modelProvider) {
      case "groq" -> waitFor(30);
      default -> RequestObserver.NONE;
    };
  }

  private static RequestObserver waitFor(final int seconds) {
    return ctx -> {
      try {
        TimeUnit.SECONDS.sleep(seconds);
      } catch (InterruptedException e) {
        log.error("Could not to sleep {} seconds", seconds, e);
      }
    };
  }

}
