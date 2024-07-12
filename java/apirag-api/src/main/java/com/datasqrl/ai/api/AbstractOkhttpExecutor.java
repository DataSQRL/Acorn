package com.datasqrl.ai.api;

import com.datasqrl.ai.util.ErrorHandling;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@SuperBuilder
@AllArgsConstructor
@Getter
public abstract class AbstractOkhttpExecutor implements APIExecutor {

  protected static final ObjectMapper objectMapper = new ObjectMapper();

  protected final OkHttpClient httpClient = new OkHttpClient();
  protected final String endpoint;
  protected final String authHeader;

  protected abstract Request buildRequest(APIQuery query, JsonNode arguments) throws IOException;


  @Override
  public String executeQuery(APIQuery query, JsonNode arguments) throws IOException {
    Request request = buildRequest(query, arguments);
    log.debug("Executing query: {}", request);

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error("Query failed: {}", response);
        throw new IOException("Query failed: " + response);
      }
      return response.body().string();
    }
  }

  @Override
  public CompletableFuture<String> executeQueryAsync(APIQuery query, JsonNode arguments) throws IOException {
    Request request = buildRequest(query, arguments);
    CompletableFuture<String> future = new CompletableFuture<>();

    httpClient.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        future.completeExceptionally(e);
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        if (!response.isSuccessful()) {
          future.completeExceptionally(new IOException("Query failed: " + response));
        } else {
          future.complete(response.body().string());
        }
      }
    });

    return future;
  }

}