package com.datasqrl.ai.backend;

import lombok.Getter;

@Getter
public enum FunctionType {

  /**
   * Function is executed by calling an API
   */
  api(false),
  /**
   * Function is passed through to the client for execution
   */
  client(true),
  /**
   * Function is executed locally
   */
  local(false);

  private final boolean isClientExecuted;

  FunctionType(boolean isClientExecuted) {
    this.isClientExecuted = isClientExecuted;
  }
}
