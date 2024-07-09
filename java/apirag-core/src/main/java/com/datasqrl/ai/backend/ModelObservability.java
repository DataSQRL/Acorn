package com.datasqrl.ai.backend;

import java.util.Map;

public interface ModelObservability {

  Trace start();

  void printMetrics();

  public interface Trace {

    void stop();

    void complete(int numInputTokens, int numOutputTokens, boolean retry);

  }

  public static final ModelObservability NOOP = new ModelObservability() {
    @Override
    public Trace start() {
      return new Trace() {
        @Override
        public void stop() {

        }

        @Override
        public void complete(int numInputTokens, int numOutputTokens, boolean retry) {

        }
      };
    }

    @Override
    public void printMetrics() {

    }
  };

}
