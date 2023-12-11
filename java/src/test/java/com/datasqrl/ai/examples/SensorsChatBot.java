package com.datasqrl.ai.examples;

import com.datasqrl.ai.CmdLineChatBot;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.APIChatBackend;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;

/**
 * An example chatbot for the <a href="https://www.datasqrl.com/docs/getting-started/quickstart/">DataSQRL Sensors</a> example
 * that can answer questions about the sensor readings and help analyze the data.
 *
 * Before you run this chatbot, make sure you have the Sensors API running.
 * See {@code api-examples/sensors} for more information.
 *
 * As with all examples, you need to export your {@code OPENAI_TOKEN} as an environment variable.
 */
public class SensorsChatBot {

  public static final String DEFAULT_GRAPHQL_ENDPOINT = "http://localhost:8888/graphql";

  public static void main(String... args) throws Exception {
    String openAIToken = System.getenv("OPENAI_TOKEN");
    String graphQLEndpoint = DEFAULT_GRAPHQL_ENDPOINT;
    if (args!=null && args.length>0) graphQLEndpoint = args[0];

    GraphQLExecutor apiExecutor = new GraphQLExecutor(graphQLEndpoint);
    APIChatBackend backend = APIChatBackend.of(Path.of("../api-examples/sensors/sensors.tools.json"), apiExecutor, Map.of());

    CmdLineChatBot chatBot = new CmdLineChatBot(openAIToken, backend);
    chatBot.start("You help users retrieve sensor data and analyse it using the available function calls. "
        + "answer questions about their orders and shopping. "
        + "All your answers are based on the specific information retrieved through function calls. You don't provide general answers. "
        + "You talk like Marvin from Hitchhiker's Guide to the Galaxy.");
  }

}