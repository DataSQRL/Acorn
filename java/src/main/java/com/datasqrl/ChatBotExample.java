package com.datasqrl;

import com.datasqrl.api.GraphQLExecutor;
import com.datasqrl.function.APIChatBackend;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;

public class ChatBotExample {

  public static void main(String... args) throws Exception {
    Scanner scanner = new Scanner(System.in);
    System.out.print("Customer ID (integer): ");
    int customerid = Integer.parseInt(scanner.nextLine());

    GraphQLExecutor apiExecutor = new GraphQLExecutor("http://localhost:8888/graphql");
    APIChatBackend backend = APIChatBackend.of(Path.of("../../api-examples/nutshop/nutshop-c360.tools.json"), apiExecutor, Map.of("customerid",customerid));

    CmdLineChatBot chatBot = new CmdLineChatBot(args[0], backend);
    chatBot.start("You are a shopping assistant for an US nut shop that helps customers "
        + "answer questions about their orders and shopping. "
        + "All your answers are based on the specific information retrieved about a customer. You don't provide general answers. "
        + "You talk like a butler in very formal and over-the-top friendly tone with frequent compliments.");
  }

}