package com.datasqrl.ai.trace;

import com.datasqrl.ai.tool.Context;

@FunctionalInterface
public interface RequestObserver {

  public static final RequestObserver NONE = (context) -> {};

  void observe(Context context);

}
