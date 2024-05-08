package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.models.ChatMessageFormatter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Llama3MessageFormatter implements ChatMessageFormatter {

  public String formatMessage(BedrockChatRole role, String content) {
    return switch (role) {
      case USER -> "<|start_header_id|>user<|end_header_id|>\n"
          + content + "\n"
          + "<|eot_id|>\n";
      case ASSISTANT -> "<|start_header_id|>assistant<|end_header_id|>\n"
          + content + "\n"
          + "<|eot_id|>\n";
      case FUNCTION ->"<|start_header_id|>function<|end_header_id|>\n"
          + content + "\n"
          + "<|eot_id|>\n";
      case SYSTEM -> "<|begin_of_text|>\n"
          + "<|start_header_id|>system<|end_header_id|>\n"
          + content + "\n"
          + "<|eot_id|>\n";
    };
  }
}
