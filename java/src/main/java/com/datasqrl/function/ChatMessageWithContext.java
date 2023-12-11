package com.datasqrl.function;

import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageWithContext {

  String role;
  String content;
  String name;
  Map<String,Object> context;

  public static ChatMessageWithContext of(ChatMessage msg, Map<String, Object> context) {
    return ChatMessageWithContext.builder()
        .role(msg.getRole())
        .content(msg.getFunctionCall() == null? msg.getContent(): functionCall2String(msg.getFunctionCall()))
        .name(msg.getName())
        .context(context)
        .build();
  }

  private static String functionCall2String(ChatFunctionCall fctCall) {
    return fctCall.getName() + "@" + fctCall.getArguments().toPrettyString();
  }

}
