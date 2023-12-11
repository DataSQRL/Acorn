package com.datasqrl.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface APIExecutor {

  String executeQuery(String query, JsonNode arguments) throws IOException;

  CompletableFuture<String> executeWrite(String graphqlMutation, JsonNode variables);

}
