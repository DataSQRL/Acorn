package com.datasqrl.ai.models.vertex;

import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.tool.ToolsBackend;
import com.google.auto.service.AutoService;
import org.apache.commons.configuration2.Configuration;

@AutoService(ChatProviderFactory.class)
public class VertexChatProviderFactory implements ChatProviderFactory {

  public static final String PROVIDER_NAME = "vertex";

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  @Override
  public ChatProvider<?, ?> create(Configuration modelConfiguration, ToolsBackend backend, String prompt) {
    return new VertexChatProvider(new VertexModelConfiguration(modelConfiguration), backend, prompt);
  }
}
