package com.datasqrl.ai.tool;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;

/**
 * The {@link Context} class captures the context of an agent interaction.
 * It has a request id that is unique for each interaction and an invocation counter
 * for the number of times the LLM is invoked in the course of producing a request response.
 *
 * Additional key-value pairs can be provided to securely pass information to the function
 * calls outside the LLM call stack.
 *
 * The request id and secure information are static for the duration of an interaction.
 * The counter is incremented for each time the LLM is invoked.
 */
@Value
@JsonSerialize(using = Context.ContextSerializer.class)
@JsonDeserialize(using = Context.ContextDeserializer.class)
@AllArgsConstructor
public class Context {

  public static final String REQUEST_ID_KEY = "requestid";
  public static final String INVOCATION_KEY = "invocationid";

  String requestId;
  int invocationId;
  Map<String, Object> secure;

  public Object get(String key) {
    if (key.equalsIgnoreCase(REQUEST_ID_KEY)) return requestId;
    if (key.equalsIgnoreCase(INVOCATION_KEY)) return invocationId;
    return secure.get(key);
  }

  public void forEach(BiConsumer<String, Object> action) {
    Objects.requireNonNull(action);
    action.accept(REQUEST_ID_KEY, requestId);
    action.accept(INVOCATION_KEY, invocationId);
    secure.forEach(action);
  }

  public Map<String,Object> asMap() {
    Map<String, Object> result = new HashMap<>(secure.size()+2);
    forEach(result::put);
    return result;
  }

  public Context next() {
    return new Context(requestId, invocationId + 1, secure);
  }

  public static Context of() {
    return of(Collections.emptyMap());
  }

  public static Context of(Map<String, Object> secure) {
    return new Context(UUID.randomUUID().toString(), 0, secure);
  }

  public static class ContextSerializer extends StdSerializer<Context> {
    public ContextSerializer() {
      this(null);
    }

    public ContextSerializer(Class<Context> t) {
      super(t);
    }

    @Override
    public void serialize(Context context, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeStartObject();
      for (Map.Entry<String, Object> field : context.asMap().entrySet()) {
        gen.writeObjectField(field.getKey(), field.getValue());
      }
      gen.writeEndObject();
    }
  }

  public static class ContextDeserializer extends StdDeserializer<Context> {

    public ContextDeserializer() {
      this(null);
    }

    public ContextDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public Context deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
      Map<String, Object> map = jsonParser.readValueAs(Map.class);
      String requestId = (String) map.get(REQUEST_ID_KEY);
      Integer invocationId = (Integer) map.get(INVOCATION_KEY);

      map.remove(REQUEST_ID_KEY);  // Remove non-secure keys before putting into secure
      map.remove(INVOCATION_KEY);

      return new Context(requestId, invocationId, map);
    }
  }

}
