package com.datasqrl.ai.api;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class APIExecutorTest {

  @Test
  public void testBuilder() {
    GraphQLExecutor graphql = GraphQLExecutor.builder().endpoint("localhost").build();
    RESTExecutor rest = RESTExecutor.builder().endpoint("localhost").authHeader("test").build();
  }

}
