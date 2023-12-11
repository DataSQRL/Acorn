package com.datasqrl.ai.examples;

import com.datasqrl.ai.CmdLineChatBot;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.APIChatBackend;
import java.nio.file.Path;
import java.util.Map;

/**
 * An example chatbot that answers questions about the Rick and Morty show by
 * querying the <a href="https://rickandmortyapi.com/">Rick and Morty API</a>.
 * The chatbot can answer questions about characters and episodes.
 * This chatbot does not persist message history.
 *
 * As with all examples, you need to export your {@code OPENAI_TOKEN} as an environment variable.
 */
public class RickAndMortyChatBot {

  public static final String DEFAULT_GRAPHQL_ENDPOINT = "https://rickandmortyapi.com/graphql";

  public static void main(String... args) throws Exception {
    String openAIToken = System.getenv("OPENAI_TOKEN");
    GraphQLExecutor apiExecutor = new GraphQLExecutor(DEFAULT_GRAPHQL_ENDPOINT);
    APIChatBackend backend = APIChatBackend.of(Path.of("../api-examples/rickandmorty/rickandmorty.tools.json"), apiExecutor, Map.of());

    CmdLineChatBot chatBot = new CmdLineChatBot(openAIToken, backend);
    chatBot.start("You are a huge fan of the Ricky and Morty TV show and help users answer questions "
        + "about the show. "
        + "You always try to look up the information a user is asking for via one of the available functions. "
        + "Only if you cannot find the information do you use general knowledge to answer it. Retrieved information always takes precedence."
        + "You answer in the voice of Jerry Smith.");
  }

}