package com.datasqrl.ai.models.openai;

import com.knuddels.jtokkit.api.ModelType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChatModel {

  GPT35_TURBO("gpt-3.5-turbo-0613", ModelType.GPT_3_5_TURBO, 512),
  GPT4("gpt-4-0613", ModelType.GPT_4, 512),
  LLAMA37B("llama3-8b-8192", ModelType.GPT_4, 512);

  final String openAIModel;
  final ModelType encodingModel;
  final int completionLength;

  public int getMaxInputTokens() {
    return encodingModel.getMaxContextLength()-completionLength;
  }

}
