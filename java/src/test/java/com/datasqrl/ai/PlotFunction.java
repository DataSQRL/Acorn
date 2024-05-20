package com.datasqrl.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PlotFunction {

  none(""), oneD("plotfunction1d.json"), twoD("plotfunction2d.json");

  private final String resourceFile;

  public boolean isPresent() {
    return this!=none;
  }


}
