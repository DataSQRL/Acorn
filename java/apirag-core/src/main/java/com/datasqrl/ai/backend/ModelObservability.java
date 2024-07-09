package com.datasqrl.ai.backend;

public interface ModelObservability {

  Trace start();

  String exportToCSV();

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
    public String exportToCSV() {
      return "";
    }
  };

}
