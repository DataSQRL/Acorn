package com.datasqrl.ai.api;

import com.google.auto.service.AutoService;
import org.apache.commons.configuration2.Configuration;

@AutoService(APIExecutorFactory.class)
public class SpringJDBCExecutorFactory implements APIExecutorFactory {

  public static final String TYPE = "jdbc";

  public static final String URL_KEY = "url";
  public static final String DRIVER_CLASS_KEY = "driverClass";
  public static final String USERNAME_KEY = "username";
  public static final String PASSWORD_KEY = "password";

  @Override
  public String getTypeName() {
    return TYPE;
  }

  @Override
  public APIExecutor create(Configuration configuration, String name) {
    String url = configuration.getString(URL_KEY);
    String driverClass = configuration.getString(DRIVER_CLASS_KEY);
    String username = configuration.getString(USERNAME_KEY);
    String password = configuration.getString(PASSWORD_KEY);
    return new SpringJDBCExecutor(url, driverClass, username, password);
  }
}