package com.datasqrl.ai;

import com.knuddels.jtokkit.api.ModelType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChatModel {

  GPT35_TURBO("gpt-3.5-turbo-0613", ModelType.GPT_3_5_TURBO, 512);

  final String openAIModel;
  final ModelType encodingModel;
  final int completionLength;

  public int getMaxInputTokens() {
    return encodingModel.getMaxContextLength()-completionLength;
  }

}
