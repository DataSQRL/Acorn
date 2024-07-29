package com.datasqrl.ai.models;

import com.datasqrl.ai.tool.Context;
import com.datasqrl.ai.tool.ModelObservability;
import com.datasqrl.ai.tool.ToolManager;
import com.datasqrl.ai.tool.ToolsBackend;
import com.datasqrl.ai.tool.GenericChatMessage;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface ChatProvider {

  GenericChatMessage chat(String message, Context context);

  List<GenericChatMessage> getHistory(Context sessionContext, boolean includeFunctionCalls);

}
