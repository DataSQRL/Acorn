package com.datasqrl.ai.models;

public interface ModelConfiguration {

  String getModelName();

  int getMaxInputTokens();

  int getMaxOutputTokens();

  double getTemperature();

  double getTopP();

}
