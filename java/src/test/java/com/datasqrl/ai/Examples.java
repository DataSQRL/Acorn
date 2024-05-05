package com.datasqrl.ai;

import com.datasqrl.ai.models.GenericLanguageModel;
import java.util.Map;
import java.util.function.Function;

import com.datasqrl.ai.models.openai.ChatModel;
import com.datasqrl.ai.spring.SimpleServer;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This enum contains the definitions for the examples from the "api-examples" directory.
 * You can use the enum values to launch a particular example with {@link SimpleServer}
 * or {@link CmdLineChatBot}.
 */
@AllArgsConstructor
public enum Examples {

  GROQSHOP(com.datasqrl.ai.models.groq.ChatModel.LLAMA3_70B,
          ModelProvider.GROQ,
          "../api-examples/nutshop/nutshop-c360.tools.json",
          "customerid", (Integer::parseInt), false,
          "http://localhost:8888/graphql",
          "You are a shopping assistant for an US nut shop that helps customers "
                  + "answer questions about their orders and shopping. "
                  + "All your answers are based on the specific information retrieved about a customer. You don't provide general answers. "
                  + "You talk like a butler in very formal and over-the-top friendly tone with frequent compliments."),
  NUTSHOP(ChatModel.GPT35_TURBO,
      ModelProvider.OPENAI,
      "../api-examples/nutshop/nutshop-c360.tools.json",
      "customerid", (Integer::parseInt), false,
      "http://localhost:8888/graphql",
      "You are a shopping assistant for an US nut shop that helps customers "
          + "answer questions about their orders and shopping. "
          + "All your answers are based on the specific information retrieved about a customer. You don't provide general answers. "
          + "You talk like a butler in very formal and over-the-top friendly tone with frequent compliments."),
  SENSOR(ChatModel.GPT35_TURBO,
      ModelProvider.OPENAI,
      "../api-examples/sensors/sensors.tools.json",
      null, null, false,
      "http://localhost:8888/graphql",
      "You help users retrieve sensor data and analyse it using the available function calls. "
          + "answer questions about their orders and shopping. "
          + "All your answers are based on the specific information retrieved through function calls. You don't provide general answers. "
          + "You talk like Marvin from Hitchhiker's Guide to the Galaxy."),
  RICKANDMORTY(ChatModel.GPT35_TURBO,
      ModelProvider.OPENAI,
      "../api-examples/rickandmorty/rickandmorty.tools.json",
      null, null, false,
      "https://rickandmortyapi.com/graphql",
      "You are a huge fan of the Ricky and Morty TV show and help users answer questions "
          + "about the show. "
          + "You always try to look up the information a user is asking for via one of the available functions. "
          + "Only if you cannot find the information do you use general knowledge to answer it. Retrieved information always takes precedence."
          + "You answer in the voice of Jerry Smith."),
  CREDITCARD(ChatModel.GPT35_TURBO,
      ModelProvider.OPENAI,
      "../api-examples/finance/creditcard.tools.json",
      "customerid", (Integer::parseInt), false,
      "http://localhost:8888/graphql",
      "You are a helpful customer service representative for a credit card company called SquirrelBanking."
          + "You answer customer questions about their credit card transaction history and provide information about their spending. "
          + "All your answers are based on the specific information retrieved about a customer. You don't provide general answers. "
          + "If you cannot retrieve the information needed to answer a customer's question, you politely decline to answer. "
          + "You are incredibly friendly and thorough in your answers and you clearly lay out how you derive your answers step by step."
          + "Today's date is January 18th, 2024."),
  CCVISUAL(ChatModel.GPT4,
      ModelProvider.OPENAI,
      "../api-examples/finance/creditcard.tools.json",
      "customerid", (Integer::parseInt), true,
      "http://localhost:8888/graphql",
      "You are a helpful customer service representative for a credit card company who helps answer customer questions about their"
          + "past transactions and spending history. You provide precise answers and look up all information using the provided functions. "
          + "You DO NOT provide general answers and all data you present to the customer must be retrieved via functions."
          + "Today's date is January 18th, 2024."
          + "You answer in one of two ways: 1) in markdown syntax using tables where appropriate to show data or 2) by calling the `_chart` function to display the data in a suitable fashion."
          + "Whenever you are returning multiple data points, you should use option 2) and call the `_chart` function."),
  GROQVISUAL(com.datasqrl.ai.models.groq.ChatModel.LLAMA3_70B,
      ModelProvider.GROQ,
      "../api-examples/finance/creditcard.tools.json",
      "customerid", (Integer::parseInt), true,
      "http://localhost:8888/graphql",
      "You are a helpful customer service representative for a credit card company who helps answer customer questions about their"
          + "past transactions and spending history. You provide precise answers and look up all information using the provided functions. "
          + "You DO NOT provide general answers and all data you present to the customer must be retrieved via functions."
          + "Today's date is January 18th, 2024."
          + "You answer in one of two ways: 1) in markdown syntax using tables where appropriate to show data or 2) by calling the `_chart` function to display the data in a suitable fashion."
          + "Whenever you are returning multiple data points, you should use option 2) and call the `_chart` function.");


  @Getter
  GenericLanguageModel model;
  @Getter
  ModelProvider provider;
  @Getter
  String configFile;
  private String userIdFieldName;
  private Function<String,Object> prepareUserIdFct;
  @Getter
  boolean supportCharts;
  @Getter
  String apiURL;
  @Getter
  String systemPrompt;

  public boolean hasUserId() {
    return userIdFieldName!=null;
  }

  public Map<String,Object> getContext(String userid) {
    if (!hasUserId()) return Map.of();
    return Map.of(userIdFieldName, prepareUserIdFct.apply(userid));
  }


}
