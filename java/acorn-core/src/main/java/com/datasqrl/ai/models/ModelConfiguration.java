package com.datasqrl.ai.models;

public interface ModelConfiguration {

  String getModelName();

  String getTokenizerName();

  int getMaxInputTokens();

  boolean hasMaxOutputTokens();

  Integer getMaxOutputTokens();

  double getTemperature();

  double getTopP();

}
