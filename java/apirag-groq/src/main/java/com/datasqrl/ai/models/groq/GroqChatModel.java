package com.datasqrl.ai.models.groq;

import com.datasqrl.ai.models.GenericLanguageModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GroqChatModel implements GenericLanguageModel {

  LLAMA3_70B("llama3-70b-8192", "meta-llama/Meta-Llama-3-70B-Instruct", 8192, 512),
  MIXTRAL_8x7B("mixtral-8x7b-32768", "mistralai/Mixtral-8x7B-Instruct-v0.1", 32768, 512),
  GEMMA_7B("gemma-7b-it", "google/gemma-1.1-7b-it", 8192, 512),
  LLAMA3_7B("llama3-8b-8192", "meta-llama/Meta-Llama-3-8B-Instruct", 8192, 512);

  final String modelName;
  final String tokenizerName;
  final int contextWindowLength;
  final int completionLength;

}
