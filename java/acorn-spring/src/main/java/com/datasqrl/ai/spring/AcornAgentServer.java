package com.datasqrl.ai.spring;

import com.datasqrl.ai.config.AcornAgentConfiguration;
import com.datasqrl.ai.config.ContextConversion;
import com.datasqrl.ai.models.ChatProvider;
import com.datasqrl.ai.tool.Context;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
      log.error("Application failed to start", e);
    }
  }

  @RestController
  public static class MessageController {

    private final ChatProvider<?, ?> chatProvider;
    private final List<String> contextKeys;

    @SneakyThrows
    public MessageController(AcornAgentServerProperties props) {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(props.getConfig()),"Need to provide a configuration file");
      Preconditions.checkArgument(!Strings.isNullOrEmpty(props.getTools()), "Need to provide a tools file");
      AcornAgentConfiguration configuration = AcornAgentConfiguration.fromFile(Path.of(props.getConfig()), Path.of(props.getTools()));
      this.contextKeys = configuration.getContext();
      this.chatProvider = configuration.getChatProvider();
    }

    @GetMapping("/messages")
    public List<ResponseMessage> getMessages(@RequestParam String userId) {
      Context context = ContextConversion.getContextFromUserId(userId, contextKeys);
      return chatProvider.getHistory(context, false).stream().map(ResponseMessage::from).toList();
    }

    @PostMapping("/messages")
    public ResponseMessage postMessage(@RequestBody InputMessage message) {
      log.info("\nUser #{}: {}", message.getUserId(), message.getContent());
      Context context = ContextConversion.getContextFromUserId(message.getUserId(), contextKeys);
      return ResponseMessage.from(chatProvider.chat(message.getContent(), context));
    }
  }
}
