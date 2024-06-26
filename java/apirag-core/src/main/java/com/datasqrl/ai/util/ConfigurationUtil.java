package com.datasqrl.ai.util;

import com.datasqrl.ai.api.APIExecutor;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.configuration2.Configuration;

public class ConfigurationUtil {

  public static <T extends Enum<T>> Optional<T> getEnumFromString(Class<T> enumClazz, String name) {
    if (enumClazz != null && name != null && !name.isEmpty()) {
      for (T val :enumClazz.getEnumConstants()) {
        if (val.name().equalsIgnoreCase(name)) return Optional.of(val);
      }
    }
    return Optional.empty();
  }

  public static Set<String> getSubKeys(Configuration config) {
    Set<String> keys = new HashSet<>();
    Iterator<String> keyIter = config.getKeys();
    while (keyIter.hasNext()) {
      String key =  keyIter.next();
      keys.add(key.split("\\.")[0]);
    }
    return keys;
  }

}
