package com.datasqrl.ai.backend;

import java.util.List;
import java.util.Set;
import lombok.Data;

/**
 * Data class that defines a function with additional metadata for how
 * the function gets executed against the API and what context it requires.
 *
 * @see FunctionDefinition
 */
@Data
public class APIFunctionDefinition {

  private String type;
  private FunctionDefinition function;
  private List<String> context;
  private APIFunctionQuery api;

  public String getName() {
    return function.getName();
  }

  /**
   *
   * @return A {@link FunctionDefinition} for this function that can be invoked by a language model.
   */
  public FunctionDefinition getChatFunction() {
    return function.removeContext(Set.copyOf(context));
  }


}
