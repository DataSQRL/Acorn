package com.datasqrl.ai.examples;

import com.datasqrl.ai.CmdLineChatBot;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.APIChatBackend;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;

/**
 * An example chatbot for the <a href="">DataSQRL Nutshop</a> that can
 * answer questions about a customer's orders, their spending, and recommend products for them to buy.
 *
 * Before you run this chatbot, make sure you have the Nutshop C360 API running.
 * See {@code api-examples/nutshop} for more information.
 *
 * As with all examples, you need to export your {@code OPENAI_TOKEN} as an environment variable.
 */
public class NutshopChatBot {

  public static final String DEFAULT_GRAPHQL_ENDPOINT = "http://localhost:8888/graphql";

  public static void main(String... args) throws Exception {
    String openAIToken = System.getenv("OPENAI_TOKEN");
    String graphQLEndpoint = DEFAULT_GRAPHQL_ENDPOINT;
    if (args!=null && args.length>0) graphQLEndpoint = args[0];

    Scanner scanner = new Scanner(System.in);
    System.out.print("Enter the Customer ID (integer): ");
    int customerid = Integer.parseInt(scanner.nextLine());

    GraphQLExecutor apiExecutor = new GraphQLExecutor(graphQLEndpoint);
    APIChatBackend backend = APIChatBackend.of(Path.of("../api-examples/nutshop/nutshop-c360.tools.json"), apiExecutor, Map.of("customerid",customerid));

    CmdLineChatBot chatBot = new CmdLineChatBot(openAIToken, backend);
    chatBot.start("You are a shopping assistant for an US nut shop that helps customers "
        + "answer questions about their orders and shopping. "
        + "All your answers are based on the specific information retrieved about a customer. You don't provide general answers. "
        + "You talk like a butler in very formal and over-the-top friendly tone with frequent compliments.");
  }

}