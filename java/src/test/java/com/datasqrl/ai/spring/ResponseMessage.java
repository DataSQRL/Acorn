package com.datasqrl.ai.spring;

import com.datasqrl.ai.models.bedrock.BedrockChatMessage;
import com.datasqrl.ai.models.bedrock.BedrockChatRole;
import com.datasqrl.ai.models.bedrock.BedrockFunctionCall;
import com.fasterxml.jackson.databind.JsonNode;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.time.Instant;

import com.theokanning.openai.completion.chat.ChatMessageRole;
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


  public static ResponseMessage of(ChatMessage msg) {
    ChatFunctionCall functionCall = null;
    if (ChatMessageRole.valueOf(msg.getRole().toUpperCase()) == ChatMessageRole.ASSISTANT) {
      functionCall = ((AssistantMessage) msg).getFunctionCall();
    }
    if (functionCall != null) {
      System.out.println(functionCall.getArguments());
      return new ResponseMessage(msg.getRole(), null,
          functionCall.getArguments(), "", Instant.now().toString());
    } else {
      return new ResponseMessage(msg.getRole(),
          msg.getTextContent(),
          null,
          "",
          Instant.now().toString()
      );
    }
  }
  public static ResponseMessage of(BedrockChatMessage msg) {
    BedrockFunctionCall functionCall = null;
    if (msg.getRole() == BedrockChatRole.ASSISTANT) {
      functionCall = msg.getFunctionCall();
    }
    if (functionCall != null) {
      System.out.println(functionCall.getArguments());
      return new ResponseMessage(msg.getRole().getRole(), null,
          functionCall.getArguments(), "", Instant.now().toString());
    } else {
      return new ResponseMessage(msg.getRole().getRole(),
          msg.getTextContent(),
          null,
          "",
          Instant.now().toString()
      );
    }
  }

}
