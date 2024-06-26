package com.datasqrl.ai.api;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class GraphQLSchemaGeneratorTest {

  @Test
  @SneakyThrows
  public void test() {

    String graphQLSchema = "type Query { hello(myarg: String!): String }";
    GraphQLSchemaConverter.convert(graphQLSchema);

  }

}
