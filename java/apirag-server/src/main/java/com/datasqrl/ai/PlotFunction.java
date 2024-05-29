package com.datasqrl.ai;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PlotFunction {

  none(Optional.empty()),
  oneD(Optional.of("plotfunction1d.json")),
  twoD(Optional.of("plotfunction2d.json"));

  private final Optional<String> resourceFile;

  public boolean isPresent() {
    return this!=none;
  }


}
