package com.datasqrl.ai.spring;

import com.datasqrl.ai.config.DataAgentConfiguration;
import com.datasqrl.ai.models.ChatProvider;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Slf4j
public class AcornAgentServer {

  public static void main(String[] args) {
    try {
      SpringApplication.run(AcornAgentServer.class, args);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @RestController
  public static class MessageController {

    private final ChatProvider<?, ?> chatProvider;
    private final Function<String,Map<String,Object>> getContextFunction;

    @SneakyThrows
    public MessageController(AcornAgentServerProperties props) {
      DataAgentConfiguration configuration = DataAgentConfiguration.fromFile(Path.of(props.getConfig()), Path.of(props.getTools()));
      this.getContextFunction = configuration.getContextFunction();
      this.chatProvider = configuration.getChatProvider();
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Map<String, Object> context = getContextFunction.apply(userId);
      return chatProvider.getHistory(context, false).stream().map(ResponseMessage::from).toList();
    }

    @PostMapping("/messages")
    public ResponseMessage postMessage(@RequestBody InputMessage message) {
      log.info("\nUser #{}: {}", message.getUserId(), message.getContent());
      Map<String, Object> context = getContextFunction.apply(message.getUserId());
      return ResponseMessage.from(chatProvider.chat(message.getContent(), context));
    }
  }
}
