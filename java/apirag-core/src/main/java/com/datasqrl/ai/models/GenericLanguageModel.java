package com.datasqrl.ai.models;

public interface GenericLanguageModel {
  String getModelName();

  int getContextWindowLength();

  int getCompletionLength();
}
