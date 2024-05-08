package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.models.GenericLanguageModel;
import com.knuddels.jtokkit.api.ModelType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChatModel implements GenericLanguageModel {

  GPT35_TURBO("gpt-3.5-turbo-0613", ModelType.GPT_3_5_TURBO, 512),
  GPT4("gpt-4-0613", ModelType.GPT_4, 512);

  final String modelName;
  final ModelType encodingModel;
  final int completionLength;

  public int getMaxInputTokens() {
    return encodingModel.getMaxContextLength() - completionLength;
  }

}
