package com.datasqrl.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Implements the {@link APIExecutor} interface for GraphQL APIs using OkHTTP as the client.
 */
public class GraphQLExecutor implements APIExecutor {

  private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final OkHttpClient client;
  private final String endpoint;
  private final Optional<String> authHeader;

  private GraphQLExecutor(@NonNull String endpoint, @NonNull Optional<String> authHeader) {
    this.client = new OkHttpClient();
    this.endpoint = endpoint;
    this.authHeader = authHeader;
  }

  public GraphQLExecutor(String endpoint, @NonNull String authHeader) {
    this(endpoint, Optional.of(authHeader));
  }

  public GraphQLExecutor(String endpoint) {
    this(endpoint, Optional.empty());
  }

  @Override
  public String executeQuery(String readQuery, JsonNode arguments) throws IOException {
    Request request = buildRequest(readQuery, arguments);

    // Execute the request
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) throw new IOException("Query failed: " + response);
      return response.body().string();
    }
  }

  @Override
  public CompletableFuture<String> executeWrite(String writeQuery, JsonNode arguments) {
    Request request = buildRequest(writeQuery, arguments);

    final CompletableFuture<String> future = new CompletableFuture<>();
    client.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        future.completeExceptionally(e);
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        if (!response.isSuccessful()) future.completeExceptionally(new IOException("Query failed " + response));
        else future.complete(response.body().string());
      }
    });
    return future;
  }

  private Request buildRequest(String query, JsonNode arguments) {
    JsonNode requestBody = objectMapper.createObjectNode()
        .put("query", query)
        .set("variables", arguments);
    RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, requestBody.toString());
    Request.Builder requestBuilder = new Request.Builder()
        .url(endpoint)
        .post(body);
    authHeader.ifPresent(h -> requestBuilder.addHeader("Authorization", h));
    return requestBuilder.build();
  }

}
