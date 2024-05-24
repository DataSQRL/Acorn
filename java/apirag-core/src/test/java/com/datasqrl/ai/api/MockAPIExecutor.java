package com.datasqrl.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.NonNull;
import lombok.Value;

@Value
public class MockAPIExecutor implements APIExecutor {

  Function<String, String> queryToResult;

  public static MockAPIExecutor of(@NonNull String uniformResult) {
    return new MockAPIExecutor(s -> uniformResult);
  }

  @Override
  public String executeQuery(String readQuery, JsonNode arguments) throws IOException {
    return queryToResult.apply(readQuery);
  }

  @Override
  public CompletableFuture<String> executeWrite(String writeQuery, JsonNode arguments) {
    return CompletableFuture.completedFuture("mock write");
  }
}
