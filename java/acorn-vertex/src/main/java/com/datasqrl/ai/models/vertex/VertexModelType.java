package com.datasqrl.ai.models.vertex;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor
@Getter
public enum VertexModelType{

  GEMINI_1_5_FLASH("gemini-1.5-flash", 1000000),
  GEMINI_1_5_PRO("gemini-1.5-pro", 1000000);

  final String modelName;
  final int contextWindowLength;

  public static Optional<VertexModelType> fromName(String name) {
    for (VertexModelType modelType : VertexModelType.values()) {
      if (modelType.getModelName().equalsIgnoreCase(name)) {
        return Optional.of(modelType);
      }
    }
    return Optional.empty();
  }
}
