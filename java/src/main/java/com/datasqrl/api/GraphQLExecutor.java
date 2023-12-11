package com.datasqrl.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GraphQLExecutor implements APIExecutor {

  private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final OkHttpClient client;
  private final String endpoint;

  public GraphQLExecutor(String endpoint) {
    this.client = new OkHttpClient();
    this.endpoint = endpoint;
  }

  @Override
  public String executeQuery(String graphqlQuery, JsonNode variables) throws IOException {
    // Prepare the request body
    JsonNode requestBody = objectMapper.createObjectNode()
        .put("query", graphqlQuery)
        .set("variables", variables);

//    System.out.println("Executing query: " + requestBody.toPrettyString());

    // Create the request
    RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, requestBody.toString());
    Request request = new Request.Builder()
        .url(endpoint)
        .post(body)
        .build();

    // Execute the request
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

      // Parse and return the response body
      return response.body().string();
    }
  }

  @Override
  public CompletableFuture<String> executeWrite(String graphqlMutation, JsonNode variables) {
    // Prepare the request body
    JsonNode requestBody = objectMapper.createObjectNode()
        .put("query", graphqlMutation)
        .set("variables", variables);

    // Create the request
    RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, requestBody.toString());
    Request request = new Request.Builder()
        .url(endpoint)
        .post(body)
        .build();

    // Execute the request asynchronously
    final CompletableFuture<String> future = new CompletableFuture<>();
    client.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        future.completeExceptionally(e);
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        if (!response.isSuccessful()) future.completeExceptionally(new IOException("Unexpected code " + response));
        else future.complete(response.body().string());
      }
    });
    return future;
  }

}
