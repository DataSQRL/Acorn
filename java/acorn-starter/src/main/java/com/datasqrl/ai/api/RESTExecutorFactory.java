package com.datasqrl.ai.api;

import com.google.auto.service.AutoService;
import java.util.Optional;
import org.apache.commons.configuration2.Configuration;

@AutoService(APIExecutorFactory.class)
public class RESTExecutorFactory implements APIExecutorFactory {

  public static final String TYPE = "rest";
  public static final String AUTH_HEADERS_KEY = "auth";

  @Override
  public String getTypeName() {
    return TYPE;
  }

  @Override
  public APIExecutor create(Configuration configuration, String name) {
    BaseConfiguration baseConfiguration = APIExecutorFactory.readBaseConfiguration(configuration, name);
    return new RESTExecutor(baseConfiguration.url(), configuration.getString(AUTH_HEADERS_KEY));
  }
}
