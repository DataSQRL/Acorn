package com.datasqrl.ai.spring;

import com.datasqrl.ai.Examples;
import com.datasqrl.ai.api.GraphQLExecutor;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.backend.FunctionDefinition;
import com.datasqrl.ai.backend.FunctionType;
import com.datasqrl.ai.backend.RuntimeFunctionDefinition;
import com.datasqrl.ai.models.ChatClientProvider;
import com.datasqrl.ai.models.bedrock.BedrockChatMessage;
import com.datasqrl.ai.models.bedrock.BedrockChatModel;
import com.datasqrl.ai.models.bedrock.BedrockChatProvider;
import com.datasqrl.ai.models.groq.GroqChatModel;
import com.datasqrl.ai.models.groq.GroqChatProvider;
import com.datasqrl.ai.models.openai.OpenAiChatModel;
import com.datasqrl.ai.models.openai.OpenAiChatProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class SimpleServer {

  public static void main(String[] args) {
    try {
      SpringApplication.run(SimpleServer.class, args);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @CrossOrigin(origins = "*")
  @RestController
  public static class MessageController {

    private final Examples example;
    private ChatClientProvider chatClientProvider;
    GraphQLExecutor apiExecutor;
    FunctionBackend backend;

    @SneakyThrows
    public MessageController(@Value("${example:nutshop}") String exampleName) {
      this.example = Examples.valueOf(exampleName.trim().toUpperCase());
      String graphQLEndpoint = example.getApiURL();
      this.apiExecutor = new GraphQLExecutor(graphQLEndpoint);
      this.backend = FunctionBackend.of(Path.of(example.getConfigFile()), apiExecutor);
      if (example.getPlotFunction().isPresent()) {
        ObjectMapper objectMapper = new ObjectMapper();
        URL url = SimpleServer.class.getClassLoader().getResource(example.getPlotFunction().getResourceFile());
        if (url != null) {
          try {
            FunctionDefinition plotFunction = objectMapper.readValue(url, FunctionDefinition.class);
            this.backend.addFunction(RuntimeFunctionDefinition.builder()
                .type(FunctionType.visualize)
                .function(plotFunction)
                .context(List.of())
                .build());
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
      this.chatClientProvider = switch (example.getProvider()) {
        case OPENAI -> new OpenAiChatProvider((OpenAiChatModel) example.getModel(), example.getSystemPrompt(), backend);
        case GROQ -> new GroqChatProvider((GroqChatModel) example.getModel(), example.getSystemPrompt(), backend);
        case BEDROCK ->
            new BedrockChatProvider((BedrockChatModel) example.getModel(), example.getSystemPrompt(), backend);
      };
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Map<String, Object> context = example.getContext(userId);
      return switch (example.getProvider()) {
        case OPENAI, GROQ -> chatClientProvider.getChatHistory(context).stream().map(msg -> ProviderMessageMapper.toResponse((ChatMessage) msg)).toList();
        case BEDROCK -> chatClientProvider.getChatHistory(context).stream().map(msg -> ProviderMessageMapper.toResponse((BedrockChatMessage) msg)).toList();
      };
    }

    @PostMapping("/messages")
    public ResponseMessage postMessage(@RequestBody InputMessage message) {
      System.out.println("\nUser #" + message.getUserId() + ": " + message.getContent());
      Map<String, Object> context = example.getContext(message.getUserId());
      return switch (example.getProvider()) {
        case OPENAI, GROQ -> ProviderMessageMapper.toResponse((ChatMessage) chatClientProvider.chat(message.getContent(), context));
        case BEDROCK -> ProviderMessageMapper.toResponse((BedrockChatMessage) chatClientProvider.chat(message.getContent(), context));
      };
    }
  }
}
