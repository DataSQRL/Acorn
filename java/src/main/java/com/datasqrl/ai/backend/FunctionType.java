package com.datasqrl.ai.backend;

import lombok.Getter;

@Getter
public enum FunctionType {

  graphql(false),
  rest(false),
  visualize(true),
  local(false);

  private boolean isPassThrough;

  FunctionType(boolean isPassThrough) {
    this.isPassThrough = isPassThrough;
  }
}
