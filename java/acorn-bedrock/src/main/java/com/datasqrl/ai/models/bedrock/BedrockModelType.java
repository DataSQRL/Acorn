package com.datasqrl.ai.models.bedrock;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BedrockModelType {

  LLAMA3_70B("meta.llama3-70b-instruct-v1:0", "meta-llama/Meta-Llama-3-70B-Instruct", 8192),
  LLAMA3_8B("meta.llama3-8b-instruct-v1:0", "meta-llama/Meta-Llama-3-8B-Instruct", 8192);

  final String modelName;
  final String tokenizerName;
  final int contextWindowLength;

  public static Optional<BedrockModelType> fromName(String name) {
    for (BedrockModelType modelType : BedrockModelType.values()) {
      if (modelType.getModelName().equalsIgnoreCase(name)) {
        return Optional.of(modelType);
      }
    }
    return Optional.empty();
  }
}
