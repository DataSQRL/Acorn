package com.datasqrl.ai.api;

import com.google.auto.service.AutoService;
import org.apache.commons.configuration2.Configuration;

@AutoService(APIExecutorFactory.class)
public class MockAPIExecutorFactory implements APIExecutorFactory {

  public static final String TYPE_NAME = "mock";
  public static final String DEFAULT_RESULT_KEY = "result";
  public static final String DEFAULT_RESULT_VALUE = "mock result";

  @Override
  public String getTypeName() {
    return TYPE_NAME;
  }

  @Override
  public APIExecutor create(Configuration configuration, String name) {
    final String defaultResult = configuration.getString(DEFAULT_RESULT_KEY, DEFAULT_RESULT_VALUE);
    return new MockAPIExecutor(query -> {
      return configuration.getString(query, defaultResult);
    });
  }
}
