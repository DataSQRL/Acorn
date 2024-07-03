package com.datasqrl.ai.models.vertex;

import com.datasqrl.ai.backend.FunctionDefinition;
import com.datasqrl.ai.backend.ModelAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import lombok.SneakyThrows;

public record VertexTokenCounter(GenerativeModel model) implements ModelAnalyzer<Content> {

  @SneakyThrows
  @Override
  public int countTokens(Content content) {
    int numTokens = model.countTokens(content).getTotalTokens();
    return numTokens + numTokens / 10; //Add a 10% buffer
  }

  //TODO: This method is the same in every token counter. Move this logic to the caller and just call countTokens(String)
  @SneakyThrows
  @Override
  public int countTokens(FunctionDefinition function) {
    ObjectMapper mapper = new ObjectMapper();
    String jsonString = mapper.writeValueAsString(function);
    return countTokens(jsonString);
  }

  @SneakyThrows
  @Override
  public int countTokens(String message) {
    return model.countTokens(message).getTotalTokens();
  }

  public static VertexTokenCounter of(GenerativeModel model) {
    return new VertexTokenCounter(model);
  }
}
