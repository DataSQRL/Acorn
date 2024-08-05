package com.datasqrl.ai.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class TraceRecorderAcornAgentServer {

  public static void main(String[] args) {
    try {
      SpringApplication.run(TraceRecorderAcornAgentServer.class, args);
    } catch (Exception e) {
      log.error("Application failed to start", e);
    }
  }

//  @RestController
//  public static class TraceController {
//
//    private final ChatProvider chatProvider;
//    private final List<String> contextKeys;
//    private final Trace.TraceBuilder traceBuilder = Trace.builder();
//    private int requestIndex = 0;
//
//    @SneakyThrows
//    public TraceController(AcornAgentServerProperties props) {
//      Preconditions.checkArgument(!Strings.isNullOrEmpty(props.getConfig()), "Need to provide a configuration file");
//      Preconditions.checkArgument(!Strings.isNullOrEmpty(props.getTools()), "Need to provide a tools file");
//      AcornAgentConfiguration configuration = AcornAgentConfiguration.fromFile(Path.of(props.getConfig()), Path.of(props.getTools()));
//
//      ToolManager toolsBackend = new TraceRecordingToolManager(configuration.getToolManager(), traceBuilder, Optional.empty());
//      this.chatProvider = new TraceChatProvider(configuration.getChatProvider(toolsBackend), traceBuilder);
//      this.contextKeys = configuration.getContext();
//    }
//
//    @GetMapping("/messages")
//    public List<ResponseMessage> getMessages(@RequestParam String userId) {
//      TraceContext context = TraceContext.of(ContextConversion.getContextFromUserId(userId, contextKeys).asMap());
//      return chatProvider.getHistory(context, false).stream().map(ResponseMessage::from).toList();
//    }
//
//    @PostMapping("/messages")
//    public ResponseMessage postMessage(@RequestBody InputMessage message) {
//      log.info("\nUser #{}: {}", message.getUserId(), message.getContent());
//      if (message.getContent().equals("exit")) {
//        Trace trace = traceBuilder.build();
//        trace.writeToFile("trace-finance.json");
//        System.exit(0);
//      }
//      TraceContext context = TraceContext.of(ContextConversion.getContextFromUserId(message.getUserId(), contextKeys).asMap());
//      context.setRequestIndex(requestIndex++);
//      return ResponseMessage.from(chatProvider.chat(message.getContent(), context));
//    }
//  }
}
