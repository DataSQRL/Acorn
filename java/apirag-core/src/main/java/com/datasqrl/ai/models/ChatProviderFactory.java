package com.datasqrl.ai.models;

import com.datasqrl.ai.backend.FunctionBackend;
import org.apache.commons.configuration2.Configuration;

public interface ChatProviderFactory {

  String getProviderName();

  ChatClientProvider<?> create(Configuration modelConfiguration, String prompt, FunctionBackend backend);

}
