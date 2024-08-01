package com.datasqrl.ai.trace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Value
@Builder
public class Trace {

  @Singular
  List<Entry> entries;

  @JsonCreator
  public static Trace create(@JsonProperty("entries") List<Entry> entries) {
    return new Trace(entries);
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = Message.class, name = "Message"),
      @JsonSubTypes.Type(value = Response.class, name = "Response"),
      @JsonSubTypes.Type(value = FunctionCall.class, name = "FunctionCall"),
      @JsonSubTypes.Type(value = FunctionResponse.class, name = "FunctionResponse")
  })

  public interface Entry {

  }

  /**
   * A user input message
   */
  public record Message(int requestId, String content) implements Entry {

  }

  /**
   * A model text response
   */
  public record Response(int requestId, String content, List<EvalConfig> evals) implements Entry {

  }


  public record FunctionCall(int requestId, int invocationId, String name, boolean internal, JsonNode arguments,
                             List<EvalConfig> evals) implements Entry {

  }

  public record FunctionResponse(int requestId, int invocationId, String name, String response) implements Entry {

  }

  public record EvalConfig(String type, String field, JsonNode settings) {

  }

  @JsonIgnore
  public List<Message> getMessages() {
    return entries.stream().filter(e -> e instanceof Message).map(e -> (Message) e).toList();
  }

  public void writeToFile(String fileName) {
    ObjectMapper mapper = new ObjectMapper();
    File file = new File(fileName);
    try (FileWriter fileWriter = new FileWriter(file, true)) {
      fileWriter.write(mapper.writeValueAsString(this));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
