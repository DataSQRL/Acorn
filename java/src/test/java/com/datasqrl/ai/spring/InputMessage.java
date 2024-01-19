package com.datasqrl.ai.spring;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InputMessage {

  private String userId;
  private String content;

}
