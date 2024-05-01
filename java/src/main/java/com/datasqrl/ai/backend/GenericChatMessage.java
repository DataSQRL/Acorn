package com.datasqrl.ai.backend;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

  @JsonIgnore
  public int getNumTokens(Function<GenericChatMessage,Integer> tokenCounter) {
    if (numTokens==null) {
      numTokens=tokenCounter.apply(this);
    }
    return numTokens;
  }

}
