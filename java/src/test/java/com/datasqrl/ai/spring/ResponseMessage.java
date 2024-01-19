package com.datasqrl.ai.spring;

import com.datasqrl.ai.backend.AnnotatedChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.time.Instant;
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

  public static ResponseMessage of(AnnotatedChatMessage msg) {
    return new ResponseMessage(msg.getMessage().getRole(),
        msg.getMessage().getContent(),
        null,
        msg.hasUuid()?msg.getUuid():"",
        msg.hasTimestamp()?msg.getTimestamp().toString():""
        );
  }

  public static ResponseMessage of(ChatMessage msg) {
    ChatFunctionCall functionCall = msg.getFunctionCall();
    if (functionCall!=null) {
      System.out.println(functionCall.getArguments());
      return new ResponseMessage(msg.getRole(), null,
          functionCall.getArguments(), "", Instant.now().toString());
    } else {
      return new ResponseMessage(msg.getRole(),
          msg.getContent(),
          null,
          "",
          Instant.now().toString()
      );
    }
  }

}
