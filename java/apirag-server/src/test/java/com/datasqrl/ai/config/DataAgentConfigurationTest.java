package com.datasqrl.ai.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataAgentConfigurationTest {

  public static final Path GRAPHQL_RESOURCES = Path.of("..", "apirag-graphql", "src", "test", "resources", "graphql");
  public static final Path CORE_RESOURCES = Path.of("..", "apirag-core", "src" , "test", "resources");
  public static final Path CONFIG_RESOURCES = Path.of("src", "test", "resources", "config");

  @Test
  public void testNutshopConfiguration() {
    Path nutshopGraphQl = GRAPHQL_RESOURCES.resolve("nutshop-schema.graphqls");
    Path nutshopTools = CORE_RESOURCES.resolve("nutshop-c360.tools.json");
    Path nutshopConfig = CONFIG_RESOURCES.resolve("nutshop.config.json");
    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    DataAgentConfiguration config1 = DataAgentConfiguration.fromFile(nutshopConfig, nutshopTools, meterRegistry);
    DataAgentConfiguration config2 = DataAgentConfiguration.fromFile(nutshopConfig, nutshopGraphQl, meterRegistry);

    assertEquals(4, config1.getFunctionBackend().getFunctions().size());
    assertEquals(5, config2.getFunctionBackend().getFunctions().size());
  }

}
