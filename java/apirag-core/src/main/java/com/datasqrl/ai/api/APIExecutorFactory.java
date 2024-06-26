package com.datasqrl.ai.api;


import com.datasqrl.ai.util.ErrorHandling;
import org.apache.commons.configuration2.Configuration;

public interface APIExecutorFactory {

  public static final String NAME_KEY = "name";
  public static final String DEFAULT_NAME = "default";
  public static final String TYPE_KEY = "type";
  public static final String URL_KEY = "url";


  String getTypeName();

  APIExecutor create(Configuration configuration);


  record BaseConfiguration(String name, String type, String url) {
  }

  public static BaseConfiguration readBaseConfiguration(Configuration configuration) {
    String name = configuration.getString(NAME_KEY, DEFAULT_NAME);
    ErrorHandling.checkArgument(name!=null && !name.isBlank(), "Need to configure `%s` for each api configuration in configuration file.", NAME_KEY);
    String type = configuration.getString(TYPE_KEY);
    ErrorHandling.checkArgument(type!=null && !type.isBlank(), "Need to configure `%s` for api `%s` in configuration file.", TYPE_KEY, name);
    String url = configuration.getString(URL_KEY);
    ErrorHandling.checkArgument(url!=null && !url.isBlank(), "Need to configure `%s` for api `%s` in configuration file.", URL_KEY, name);
    return new BaseConfiguration(name, type, url);
  }

}
