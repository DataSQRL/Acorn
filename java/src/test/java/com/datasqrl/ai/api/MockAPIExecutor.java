package com.datasqrl.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class MockAPIExecutor implements APIExecutor {

  @Override
  public String executeQuery(String readQuery, JsonNode arguments) throws IOException {
    return "success";
  }

  @Override
  public CompletableFuture<String> executeWrite(String writeQuery, JsonNode arguments) {
    return CompletableFuture.completedFuture("success");
  }
}
