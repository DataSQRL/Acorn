package com.datasqrl.ai.comparison.api;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIExecutorFactory;
import com.datasqrl.ai.util.ErrorHandling;
import com.google.auto.service.AutoService;
import org.apache.commons.configuration2.Configuration;

import java.util.Optional;

@AutoService(APIExecutorFactory.class)
public class LocalAPIExecutorFactory implements APIExecutorFactory {

  public static final String TYPE = "local";
  static final String USE_CASE_KEY = "use_case";

  @Override
  public String getTypeName() {
    return TYPE;
  }

  @Override
  public APIExecutor create(Configuration configuration, String name) {
    Optional<String> useCase = Optional.ofNullable(configuration.getString(USE_CASE_KEY));
    ErrorHandling.checkArgument(useCase.isPresent(), "Need to configure `%s` in api configuration.", USE_CASE_KEY, name);
    return new LocalAPIExecutor(useCase.get());
  }
}
