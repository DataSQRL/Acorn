package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.backend.ModelAnalyzer;
import com.datasqrl.ai.backend.FunctionDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.SneakyThrows;
import lombok.Value;

@Value
public class OpenAITokenCounter implements ModelAnalyzer<ChatMessage> {

  Encoding encoding;

  public int countTokens(ChatMessage message) {
    int numTokens = countTokens(message.getTextContent());
    return numTokens + numTokens / 10; //Add a 10% buffer
  }

  private int countTokens(String message) {
    return encoding.countTokens(message);
  }


  @Override
  @SneakyThrows
  public int countTokens(FunctionDefinition function) {
    ObjectMapper mapper = new ObjectMapper();
    String jsonString = mapper.writeValueAsString(function);
    return countTokens(jsonString);
  }

  public static OpenAITokenCounter of(OpenAiChatModel model) {
    return new OpenAITokenCounter(Encodings.newDefaultEncodingRegistry().getEncodingForModel(model.getEncodingModel()));
  }
}
