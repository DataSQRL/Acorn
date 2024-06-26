package com.datasqrl.ai.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Defines the API query for a function
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class APIQuery {

  private String name;
  private String query;
  private String path;
  private String method;

  public String getNameOrDefault() {
    return name==null?APIExecutorFactory.DEFAULT_NAME:name;
  }

}
