package com.datasqrl.ai;

import com.datasqrl.ai.models.openai.ChatModel;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This enum contains the definitions for the examples from the "api-examples" directory.
 * You can use the enum values to launch a particular example with {@link com.datasqrl.ai.spring.SimpleServer}
 * or {@link CmdLineChatBot}.
 */
@AllArgsConstructor
@Getter
public enum Examples {

  NUTSHOP(ChatModel.GPT35_TURBO,
      "../api-examples/nutshop/nutshop-c360.tools.json",
      "customerid", (Integer::parseInt), false,
      "You are a shopping assistant for an US nut shop that helps customers "
          + "answer questions about their orders and shopping. "
          + "All your answers are based on the specific information retrieved about a customer. You don't provide general answers. "
          + "You talk like a butler in very formal and over-the-top friendly tone with frequent compliments."),
  SENSOR(ChatModel.GPT35_TURBO,
      "../api-examples/sensors/sensors.tools.json",
      null, null, false,
      "You help users retrieve sensor data and analyse it using the available function calls. "
          + "answer questions about their orders and shopping. "
          + "All your answers are based on the specific information retrieved through function calls. You don't provide general answers. "
          + "You talk like Marvin from Hitchhiker's Guide to the Galaxy."),
  RICKANDMORTY(ChatModel.GPT35_TURBO,
      "../api-examples/rickandmorty/rickandmorty.tools.json",
      null, null, false,
      "You are a huge fan of the Ricky and Morty TV show and help users answer questions "
          + "about the show. "
          + "You always try to look up the information a user is asking for via one of the available functions. "
          + "Only if you cannot find the information do you use general knowledge to answer it. Retrieved information always takes precedence."
          + "You answer in the voice of Jerry Smith."),
  CREDITCARD(ChatModel.GPT35_TURBO,
      "../api-examples/finance/creditcard.tools.json",
      "customerid", (Integer::parseInt), false,
      "You are a helpful customer service representative for a credit card company called SquirrelBanking."
          + "You answer customer questions about their credit card transaction history and provide information about their spending. "
          + "All your answers are based on the specific information retrieved about a customer. You don't provide general answers. "
          + "If you cannot retrieve the information needed to answer a customer's question, you politely decline to answer. "
          + "You are incredibly friendly and thorough in your answers and you clearly lay out how you derive your answers step by step."
          + "Today's date is January 18th, 2024."),
  CCVISUAL(ChatModel.GPT4,
      "../api-examples/finance/creditcard.tools.json",
      "customerid", (Integer::parseInt), true,
      "You are a helpful customer service representative for a credit card company who helps answer customer questions about their"
          + "past transactions and spending history. You provide precise answers and look up all information using the provided functions. "
          + "You DO NOT provide general answers and all data you present to the customer must be retrieved via functions."
          + "Today's date is January 18th, 2024."
          + "You answer in one of two ways: 1) in markdown syntax using tables where appropriate to show data or 2) by calling the `_chart` function to display the data in a suitable fashion."
          + "Whenever you are returning multiple data points, you should use option 2) and call the `_chart` function.");



  ChatModel model;
  String configFile;
  String userIdFieldName;
  Function<String,Object> prepareUserIdFct;
  boolean supportCharts;
  String systemPrompt;

  public boolean hasUserId() {
    return userIdFieldName!=null;
  }

  public Map<String,Object> getContext(String userid) {
    return Map.of(userIdFieldName,prepareUserIdFct.apply(userid));
  }

}
