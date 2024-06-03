package com.datasqrl.ai.backend;

import lombok.Getter;

@Getter
public enum FunctionType {

  graphql(false),
  rest(false),
  client(true),
  local(false);

  private boolean isClientExecuted;

  FunctionType(boolean isClientExecuted) {
    this.isClientExecuted = isClientExecuted;
  }
}
