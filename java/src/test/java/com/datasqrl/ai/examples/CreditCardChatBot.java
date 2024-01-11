package com.datasqrl.ai.examples;

import com.datasqrl.ai.CmdLineChatBot;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.APIChatBackend;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;

/**
 * An example chatbot that answers questions about customer's credit card transaction history
 * and provides spending analysis.
 *
 * Before you run this chatbot, make sure you have the Credit Card API running.
 * See {@code api-examples/creditcard} for more information.
 *
 * As with all examples, you need to export your {@code OPENAI_TOKEN} as an environment variable.
 */
public class CreditCardChatBot {

  public static final String DEFAULT_GRAPHQL_ENDPOINT = "http://localhost:8888/graphql";

  public static void main(String... args) throws Exception {
    String openAIToken = System.getenv("OPENAI_TOKEN");
    String graphQLEndpoint = DEFAULT_GRAPHQL_ENDPOINT;
    if (args!=null && args.length>0) graphQLEndpoint = args[0];

    Scanner scanner = new Scanner(System.in);
    System.out.print("Enter the Customer ID (1-20): ");
    int customerid = Integer.parseInt(scanner.nextLine());
    if (customerid<1 || customerid>20) throw new IllegalArgumentException("Invalid customer id: " + customerid);

    GraphQLExecutor apiExecutor = new GraphQLExecutor(graphQLEndpoint);
    APIChatBackend backend = APIChatBackend.of(Path.of("../api-examples/finance/creditcard.tools.json"), apiExecutor, Map.of("customerId",customerid));

    CmdLineChatBot chatBot = new CmdLineChatBot(openAIToken, backend);
    chatBot.start("You are a helpful customer service representative for a credit card company called SquirrelBanking."
        + "You answer customer questions about their credit card transaction history and provide information about their spending. "
        + "All your answers are based on the specific information retrieved about a customer. You don't provide general answers. "
        + "If you cannot retrieve the information needed to answer a customer's question, you politely decline to answer. "
        + "You are incredibly friendly and thorough in your answers and you clearly lay out how you derive your answers step by step.");
  }

}