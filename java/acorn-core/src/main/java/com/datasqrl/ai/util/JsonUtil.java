package com.datasqrl.ai.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.configuration2.JSONConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class JsonUtil {


  public static Optional<JsonNode> parseJson(String json) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return Optional.of(mapper.readTree(json));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static JSONConfiguration getConfiguration(Path path) {
    Configurations configs = new Configurations();
    try {
      // obtain the configuration from a JSON file
      return configs.fileBased(JSONConfiguration.class, path.toFile());
    } catch (ConfigurationException cex) {
      throw new IllegalArgumentException("Not a valid configuration file: " + path, cex);
    }
  }


}
