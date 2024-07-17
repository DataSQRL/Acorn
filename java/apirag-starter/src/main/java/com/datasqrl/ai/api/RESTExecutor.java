package com.datasqrl.ai.api;

import com.datasqrl.ai.api.RestUtil.RestCall;
import com.datasqrl.ai.util.ErrorHandling;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Optional;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

@SuperBuilder
public class RESTExecutor extends AbstractOkhttpExecutor {

  public RESTExecutor(@NonNull String endpoint, String authHeader) {
    super(endpoint, authHeader);
  }

  @Override
  public void validate(APIQuery query) throws IllegalArgumentException {
    ErrorHandling.checkNotNullOrEmpty(query.getMethod(), "`method` cannot be empty");
    ErrorHandling.checkArgument(query.getMethod() != null);
    ErrorHandling.checkNotNullOrEmpty(query.getPath(), "`path` cannot be empty");
  }


  protected Request buildRequest(APIQuery query, JsonNode arguments) throws IOException {
    RestCall call = RestUtil.createRestCall(query, arguments);
    Request.Builder requestBuilder = new Request.Builder()
        .url(endpoint + call.path())
        .method(query.getMethod(), buildRequestBody(call.body(), query.getMethod()));

    if (!Strings.isNullOrEmpty(authHeader)) {
      requestBuilder.addHeader("Authorization", authHeader);
    }
    return requestBuilder.build();
  }

  private RequestBody buildRequestBody(JsonNode requestBody, String method) throws IOException {
    if (requestBody == null || requestBody.isEmpty() || method.equalsIgnoreCase("GET")) {
      return RequestBody.create("", null);
    }
    return RequestBody.create(objectMapper.writeValueAsString(requestBody), MediaType.get("application/json; charset=utf-8"));
  }

}