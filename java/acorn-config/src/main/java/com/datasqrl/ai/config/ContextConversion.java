package com.datasqrl.ai.config;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;

public class ContextConversion {

  public static Map<String,Object> getContextFromUserId(String userId, List<String> contextKeys) {
    if (Strings.isNullOrEmpty(userId)) {
      Preconditions.checkArgument(contextKeys.isEmpty(), "Expected a user id for context: %s", contextKeys);
      return Map.of();
    }
    if (contextKeys.isEmpty()) return Map.of();
    Preconditions.checkArgument(contextKeys.size()==1, "Expected a single context key: %s", contextKeys);
    String key = contextKeys.get(0);
    try {
      Long numericValue = Long.parseLong(userId);
      return Map.of(key, numericValue);
    } catch (NumberFormatException e) {
      //do nothing
    }
    return Map.of(key, userId);
  }

}
