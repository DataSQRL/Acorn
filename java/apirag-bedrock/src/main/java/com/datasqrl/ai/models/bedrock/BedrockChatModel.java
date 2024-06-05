package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.models.GenericLanguageModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BedrockChatModel implements GenericLanguageModel {

  LLAMA3_70B("meta.llama3-70b-instruct-v1:0", "meta-llama/Meta-Llama-3-70B-Instruct", 8192, 1024),
  LLAMA3_8B("meta.llama3-8b-instruct-v1:0", "meta-llama/Meta-Llama-3-8B-Instruct", 8192, 512);

  final String modelName;
  final String tokenizerName;
  final int contextWindowLength;
  final int completionLength;
}
