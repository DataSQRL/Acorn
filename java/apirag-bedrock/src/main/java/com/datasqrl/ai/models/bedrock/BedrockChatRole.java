package com.datasqrl.ai.models.bedrock;

import lombok.Getter;

@Getter
public enum BedrockChatRole {
  SYSTEM("system"),
  USER("user"),
  ASSISTANT("assistant"),
  FUNCTION("function");

  private final String role;

  BedrockChatRole(String role) {
    this.role = role;
  }
}
