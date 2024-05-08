package com.datasqrl.ai.backend;

import lombok.Value;

import java.util.List;

@Value
public class ChatSessionComponents<Message> {

  List<Message> messages;

  List<FunctionDefinition> functions;
}
