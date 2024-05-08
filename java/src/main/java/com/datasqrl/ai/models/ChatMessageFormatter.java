package com.datasqrl.ai.models;

import com.datasqrl.ai.models.bedrock.BedrockChatRole;

public interface ChatMessageFormatter {
  public String formatMessage(BedrockChatRole role, String content);
}
