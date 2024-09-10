package com.datasqrl.ai.trace;

import com.datasqrl.ai.tool.Context;

@FunctionalInterface
public interface RequestThrottler {

  public static final RequestThrottler NONE = (context) -> {};

  void observe(Context context);

}
