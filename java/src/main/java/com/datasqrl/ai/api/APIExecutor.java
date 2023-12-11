package com.datasqrl.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for querying and writing to an API
 */
public interface APIExecutor {

  /**
   * Executes the given query with the provided arguments against the API and returns
   * the result as a String.
   *
   * @param readQuery the read query to execute
   * @param arguments the arguments for the read query
   * @return The result of the query as a String
   * @throws IOException if the connection to the API failed or the query could not be executed
   */
  String executeQuery(String readQuery, JsonNode arguments) throws IOException;

  /**
   * Executes an asynchronous write request against the API for the given write query with arguments.
   *
   * @param writeQuery the write query to execute
   * @param arguments the arguments for the write query
   * @return A future for the result
   */
  CompletableFuture<String> executeWrite(String writeQuery, JsonNode arguments);

}
