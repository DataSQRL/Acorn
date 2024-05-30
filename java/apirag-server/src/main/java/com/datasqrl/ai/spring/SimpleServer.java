package com.datasqrl.ai.spring;

import com.datasqrl.ai.backend.ChatSession;
import com.datasqrl.ai.backend.FunctionBackend;
import com.datasqrl.ai.config.ChatBotConfiguration;
import com.datasqrl.ai.models.ChatClientProvider;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

    private final ChatClientProvider<?, ?> chatClientProvider;
    private final Function<String,Map<String,Object>> getContextFunction;

    @SneakyThrows
    public MessageController(@Value("${config}") String configFile, @Value("${tools}") String toolsFile) {
      ChatBotConfiguration configuration = ChatBotConfiguration.fromFile(Path.of(configFile), Path.of(toolsFile));
      this.getContextFunction = configuration.getContextFunction();
      this.chatClientProvider = configuration.getChatProvider();
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Map<String, Object> context = getContextFunction.apply(userId);
      return chatClientProvider.getHistory(context, false).stream().map(ResponseMessage::from).toList();
    }

    @PostMapping("/messages")
    public ResponseMessage postMessage(@RequestBody InputMessage message) {
      System.out.println("\nUser #" + message.getUserId() + ": " + message.getContent());
      Map<String, Object> context = getContextFunction.apply(message.getUserId());
      return ResponseMessage.from(chatClientProvider.chat(message.getContent(), context));
    }
  }
}
