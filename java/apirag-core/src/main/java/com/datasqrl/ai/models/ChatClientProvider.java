package com.datasqrl.ai.models;

import com.datasqrl.ai.backend.ChatSession;
import com.datasqrl.ai.backend.FunctionValidation;

import java.util.Map;

public interface ChatClientProvider<Message, FunctionCall> {

  Message chat(String message, Map<String, Object> context);

  ChatSession<Message, FunctionCall> getCurrentSession(Map<String, Object> context);
}
