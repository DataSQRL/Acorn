package com.datasqrl.ai.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.api.Encoding;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import lombok.Value;

@Value
public class MessageTruncator {

  int maxTokens;
  ChatMessage systemMessage;
  Encoding encoding;

  public List<ChatMessage> truncateMessages(List<ChatMessage> messages, List<?> functions) {
    List<ChatMessage> result = new ArrayList<>();
    int numTokens = 0;
    //First, account for functions
    if (!functions.isEmpty()) {
      ObjectMapper mapper = new ObjectMapper();
      try {
        String jsonString = mapper.writeValueAsString(functions);
        numTokens += encoding.countTokens(jsonString);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    numTokens += countTokens(systemMessage);
    if (numTokens>maxTokens) throw new IllegalArgumentException("Function calls and system message too large for model: " + numTokens);
    int numMessages = messages.size();
    ListIterator<ChatMessage> listIterator = messages.listIterator(numMessages);
    while (listIterator.hasPrevious()) {
      ChatMessage message = listIterator.previous();
      numTokens+= countTokens(message);
      if (numTokens>maxTokens) break;
      result.add(message);
      numMessages--;
    }
    result.add(systemMessage);
    Collections.reverse(result);
    if (numMessages>0) System.out.printf("Truncated the first %s messages\n", numMessages);
    return result;
  }

  public int countTokens(ChatMessage message) {
    int numTokens = encoding.countTokens(message.getContent());
    return numTokens + numTokens/10; //Add a 10% buffer
  }



}
