package com.datasqrl.ai.trace;

import com.datasqrl.ai.trace.Trace.Entry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Value
@Builder
public class Trace implements Iterable<Entry> {

  @Singular
  List<Entry> entries;

  public int size() {
    return getEntries().size();
  }

  public<E extends Entry> List<E> getAll(Class<E> clazz) {
    return entries.stream().filter(clazz::isInstance).map(clazz::cast).collect(Collectors.toUnmodifiableList());
  }

  public Response getResponse(int requestId) {
    return entries.stream().filter(e -> e.requestId()==requestId)
        .filter(Response.class::isInstance).map(Response.class::cast)
        .findFirst().orElse(null);
  }

  public FunctionCall getFunctionCall(int requestId, int invocationId) {
    List<FunctionCall> functionCalls = getFunctionCalls(requestId, invocationId);
    if (functionCalls.isEmpty()) return null;
    else if (functionCalls.size()==1) return functionCalls.get(0);
    throw new IllegalArgumentException("Model invocation produced multiple function calls. Use [getFunctionCalls] instead: " + functionCalls.toString());
  }

  public List<FunctionCall> getFunctionCalls(int requestId, int invocationId) {
    return entries.stream()
        .filter(FunctionCall.class::isInstance).map(FunctionCall.class::cast)
        .filter(f -> f.requestId()==requestId && f.invocationId()==invocationId)
        .toList();
  }

  @JsonCreator
  public static Trace create(@JsonProperty("entries") List<Entry> entries) {
    return new Trace(entries);
  }

  @Override
  public Iterator<Entry> iterator() {
    return entries.iterator();
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = Message.class, name = "Message"),
      @JsonSubTypes.Type(value = Response.class, name = "Response"),
      @JsonSubTypes.Type(value = FunctionCall.class, name = "FunctionCall"),
      @JsonSubTypes.Type(value = FunctionResponse.class, name = "FunctionResponse")
  })

  public interface Entry {
    int requestId();
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

  private static final ObjectMapper traceSerializer = new ObjectMapper();

  public void writeToFile(Path filePath) throws IOException {
    String jsonString = traceSerializer.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    Files.write(filePath, jsonString.getBytes());
  }

  public static Trace loadFromFile(Path path) throws IOException {
    return traceSerializer.readValue(path.toFile(), Trace.class);
  }

  public static Trace loadFromURL(URL url) throws IOException {
    return traceSerializer.readValue(url, Trace.class);
  }

}
