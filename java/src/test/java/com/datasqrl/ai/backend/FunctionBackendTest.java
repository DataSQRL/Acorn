package com.datasqrl.ai.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.datasqrl.ai.api.MockAPIExecutor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class FunctionBackendTest {

  @Test
  public void readNutshop() throws Exception {
    String currentDirectory = System.getProperty("user.dir");
    System.out.println("Current working directory: " + currentDirectory);
    FunctionBackend fctExec = FunctionBackend.of(Path.of("../api-examples/nutshop/nutshop-c360.tools.json"),
        MockAPIExecutor.of("none"));
    List<RuntimeFunctionDefinition> chatFcts = new ArrayList<>(fctExec.getFunctions().values());
    assertEquals(3, chatFcts.size());
    for (RuntimeFunctionDefinition function : chatFcts) {
      FunctionDefinition fct = function.getChatFunction();
      if (fct.getName().equalsIgnoreCase("orders")) {
        assertEquals(Set.of("limit"),fct.getParameters().getProperties().keySet());
        assertTrue(fct.getParameters().getRequired().isEmpty());
      } else if (fct.getName().equalsIgnoreCase("ordered_products")) {
        assertTrue(fct.getParameters().getProperties().isEmpty());
        assertTrue(fct.getParameters().getRequired().isEmpty());
      } else if (fct.getName().equalsIgnoreCase("spending_by_week")) {
        assertEquals(Set.of("limit"),fct.getParameters().getProperties().keySet());
        assertTrue(fct.getParameters().getRequired().isEmpty());
      } else {
        fail(fct.getName());
      }
    }
  }

}
