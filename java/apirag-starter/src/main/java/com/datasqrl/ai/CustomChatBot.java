package com.datasqrl.ai;

import com.datasqrl.ai.api.APIExecutor;
import com.datasqrl.ai.api.APIExecutorFactory;
import com.datasqrl.ai.api.GraphQLExecutorFactory;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.models.ChatProviderFactory;
import com.datasqrl.ai.tool.GenericChatMessage;
import com.datasqrl.ai.tool.RuntimeFunctionDefinition;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.ToolsBackendFactory;
import lombok.Value;
import org.apache.commons.configuration2.MapConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * A simple chatbot for the command line.
 * Uses a fully contained ChatProvider, configured manually.
 */
@Value
public class CustomChatBot {

  ChatProvider chatProvider;

  /**
   * Initializes a command line chatbot
   *
   * @param chatProvider    The ChatProvider to take care of the communication with the LLM and the function execution
   */
  public CustomChatBot(ChatProvider chatProvider) {
    this.chatProvider = chatProvider;
  }

  /**
   * Starts the chatbot on the command line which will accepts questions and producez responses.
   * Type "exit" to terminate.
   *
   * @param context        The user context that might be needed to execute functions
   */
  public void start(Map<String, Object> context) throws IOException {
    Scanner scanner = new Scanner(System.in);
    System.out.print("First Query: ");
    String message = scanner.nextLine();
    while (true) {
      GenericChatMessage response = chatProvider.chat(message, context);
      System.out.println("Response: " + response.getContent());
      System.out.print("Next Query: ");
      String nextLine = scanner.nextLine();
      if (nextLine.equalsIgnoreCase("exit")) {
        System.exit(0);
      }
      message = nextLine;
    }
  }

  public static void main(String... args) throws Exception {
    String systemPrompt = "You are a huge fan of the Ricky and Morty TV show and help users answer questions about the show. You always try to look up the information a user is asking for via one of the available functions. Only if you cannot find the information do you use general knowledge to answer it. Retrieved information always takes precedence. You answer in the voice of Jerry Smith.";
    List<RuntimeFunctionDefinition> tools = ToolsBackendFactory.readTools(Path.of("java", "apirag-starter", "src", "main", "resources", "tools", "rickandmorty.tools.json"));
    APIExecutor apiExecutor = new GraphQLExecutorFactory().create(new MapConfiguration(Map.of(
        "type", "graphql",
        "url", "https://rickandmortyapi.com/graphql",
        "auth", "customerid"
    )), "rickandmortyapi");
    ToolsBackend toolsBackend = ToolsBackendFactory.of(tools, Map.of(APIExecutorFactory.DEFAULT_NAME, apiExecutor));
    ChatProvider<?, ?> chatProvider = ChatProviderFactory.fromConfiguration(Map.of(
        "provider", "openai",
        "name", "gpt-3.5-turbo",
        "temperature", 0.8
    ), toolsBackend, systemPrompt);

    CustomChatBot chatBot = new CustomChatBot(chatProvider);
    Map<String, Object> context = Map.of("userid", 1);
    chatBot.start(context);
  }

}