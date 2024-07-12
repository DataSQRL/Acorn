package com.datasqrl.ai.models;

public interface ModelConfiguration {

  String getModelName();

  String getTokenizerName();

  int getMaxInputTokens();

  int getMaxOutputTokens();

  double getTemperature();

  double getTopP();

}
