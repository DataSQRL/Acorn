package com.datasqrl.ai.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.time.Instant;
import java.util.Optional;
import lombok.NonNull;
import lombok.Value;

@Value
public class AnnotatedChatMessage {

  @NonNull ChatMessage message;
  String uuid;
  Instant timestamp;

  public boolean hasUuid() {
    return uuid!=null;
  }

  public boolean hasTimestamp() {
    return timestamp!=null;
  }

  public static AnnotatedChatMessage of(ChatMessage message, JsonNode data) {
    String uuid = null;
    Instant timestamp = null;
    JsonNode uuidNode = data.get("uuid");
    if (uuidNode!=null && !uuidNode.isNull() && uuidNode.isTextual()) {
      uuid = uuidNode.textValue();
    }
    JsonNode timestampNode = data.get("timestamp");
    if (timestampNode!=null && !timestampNode.isNull()) {
      if (timestampNode.isTextual()) {
        try {
          timestamp = Instant.parse(timestampNode.textValue());
        } catch (IllegalArgumentException ex) {
          System.out.println(ex);
        }
      } //TODO: parse DateTime
    }
    return new AnnotatedChatMessage(message, uuid, timestamp);
  }

}
