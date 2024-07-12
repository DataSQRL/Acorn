package com.datasqrl.ai.models.openai;

import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.google.auto.service.AutoService;
import org.apache.commons.configuration2.Configuration;

@AutoService(ChatProviderFactory.class)
public class OpenAiChatProviderFactory implements ChatProviderFactory {

  public static final String PROVIDER_NAME = "openai";

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  @Override
  public ChatClientProvider<?, ?> create(Configuration modelConfiguration, FunctionBackend backend, String prompt) {
    return new OpenAiChatProvider(new OpenAIModelConfiguration(modelConfiguration), backend, prompt);
  }
}
