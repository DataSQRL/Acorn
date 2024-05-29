package com.datasqrl.ai.util;

import java.util.Optional;

public class ConfigurationUtil {

  public static <T extends Enum<T>> Optional<T> getEnumFromString(Class<T> enumClazz, String name) {
    if (enumClazz != null && name != null && !name.isEmpty()) {
      for (T val :enumClazz.getEnumConstants()) {
        if (val.name().equalsIgnoreCase(name)) return Optional.of(val);
      }
    }
    return Optional.empty();
  }
}
