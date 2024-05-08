package com.datasqrl.ai.models.bedrock;

import lombok.Value;

@Value
public class BedrockChatMessage {
  BedrockChatRole role;
  String textContent;
  String name;
}
