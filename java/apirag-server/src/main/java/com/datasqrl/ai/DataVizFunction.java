package com.datasqrl.ai;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DataVizFunction {

  none(Optional.empty()),
  oneD(Optional.of("functions/plotfunction1d.json")),
  twoD(Optional.of("functions/plotfunction2d.json"));

  private final Optional<String> resourceFile;

  public boolean isPresent() {
    return this!=none;
  }


}
