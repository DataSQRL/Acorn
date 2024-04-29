package com.datasqrl.ai.backend;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class GenericChatMessage implements ChatMessageInterface {

  String role;
  String content;
  String name;
  Map<String,Object> context;
  String uuid;
  String timestamp;
  Integer numTokens;


  @JsonIgnore
  public Instant getTimestampInstant() {
    if (timestamp==null || timestamp.isBlank()) return null;
    else return Instant.parse(timestamp);
  }

}
