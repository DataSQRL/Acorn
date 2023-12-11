package com.datasqrl.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class APIChatBackendTest {

  @Test
  public void readTest() throws Exception {
    String currentDirectory = System.getProperty("user.dir");
    System.out.println("Current working directory: " + currentDirectory);
    APIChatBackend fctExec = APIChatBackend.of(Path.of("../../api-examples/nutshop/nutshop-c360.tools.json"), null, Map.of("customerid", 2));
    List<FunctionDefinition> chatFcts = fctExec.getChatFunctions();
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(chatFcts);
    System.out.println(json);
  }

}
