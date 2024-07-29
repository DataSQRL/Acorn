package com.datasqrl.ai.comparison.trace;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class Trace {

  @Singular
  List<Entry> entries;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = Message.class, name = "Message"),
      @JsonSubTypes.Type(value = Response.class, name = "Response"),
      @JsonSubTypes.Type(value = FunctionCall.class, name = "FunctionCall"),
      @JsonSubTypes.Type(value = FunctionResponse.class, name = "FunctionResponse")
  })

  interface Entry {

  }

  /**
     * A user input message
     */
  record Message(int requestId, String content) implements Entry {

  }

  /**
     * A model text response
     */
  record Response(int requestId, String content, List<EvalConfig> evals) implements Entry {

  }


  record FunctionCall(int requestId, int invocationId, String name, boolean internal, JsonNode arguments,
                      List<EvalConfig> evals) implements Entry {

  }

  record FunctionResponse(int requestId, int invocationId, String name, String response) implements Entry {

  }

  record EvalConfig(String type, String field, JsonNode settings) {

  }

}
