package com.datasqrl.ai.api;

import com.datasqrl.ai.util.ErrorHandling;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

@SuperBuilder
public class GraphQLExecutor extends AbstractOkhttpExecutor {

  public GraphQLExecutor(@NonNull String endpoint, String authHeader) {
    super(endpoint, authHeader);
  }

  @Override
  public void validate(APIQuery query) throws IllegalArgumentException {
    ErrorHandling.checkNotNullOrEmpty(query.getQuery(), "`query` cannot be empty");
  }

  protected Request buildRequest(APIQuery query, JsonNode arguments) throws IOException {
    JsonNode requestBody = objectMapper.createObjectNode()
        .put("query", query.getQuery())
        .set("variables", arguments);

    RequestBody body = RequestBody.create(objectMapper.writeValueAsString(requestBody), MediaType.get("application/json"));

    Request.Builder requestBuilder = new Request.Builder()
        .url(endpoint)
        .post(body);

    if (!Strings.isNullOrEmpty(authHeader)) {
      requestBuilder.addHeader("Authorization", authHeader);
    }

    return requestBuilder.build();
  }
}