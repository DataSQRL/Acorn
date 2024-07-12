package com.datasqrl.ai.models.groq;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GroqModelType {

  LLAMA3_70B("llama3-70b-8192", "meta-llama/Meta-Llama-3-70B-Instruct", 8192),
  MIXTRAL_8x7B("mixtral-8x7b-32768", "mistralai/Mixtral-8x7B-Instruct-v0.1", 32768),
  GEMMA_7B("gemma-7b-it", "google/gemma-1.1-7b-it", 8192),
  GEMMA2_9B("gemma2-9b-it", "google/gemma-2-9b", 8192),
  LLAMA3_8B("llama3-8b-8192", "meta-llama/Meta-Llama-3-8B-Instruct", 8192);

  final String modelName;
  final String tokenizerName;
  final int contextWindowLength;

  public static Optional<GroqModelType> fromName(String name) {
    for (GroqModelType modelType : GroqModelType.values()) {
      if (modelType.getModelName().equalsIgnoreCase(name)) {
        return Optional.of(modelType);
      }
    }
    return Optional.empty();
  }

}
