package com.datasqrl.ai.models.bedrock;

import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.google.auto.service.AutoService;
import org.apache.commons.configuration2.Configuration;

@AutoService(ChatProviderFactory.class)
public class BedrockChatProviderFactory implements ChatProviderFactory {

  public static final String PROVIDER_NAME = "bedrock";

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  @Override
  public ChatProvider<?, ?> create(Configuration modelConfiguration, ToolsBackend backend, String prompt) {
    return new BedrockChatProvider(new BedrockModelConfiguration(modelConfiguration), backend, prompt);
  }
}
