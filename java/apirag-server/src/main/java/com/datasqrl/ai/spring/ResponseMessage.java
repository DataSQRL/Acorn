package com.datasqrl.ai.spring;

import com.datasqrl.ai.backend.GenericChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseMessage {

  private String role;
  private String content;
  private JsonNode chart;
  private String uuid;
  private String timestamp;

  public static ResponseMessage from(GenericChatMessage message) {
    return new ResponseMessage(message.getRole(), message.getContent(),
        message.getPassThroughCall(), message.getUuid(), message.getTimestamp());
  }

}
