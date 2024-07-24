# Acorn Agent Starter

This module contains [example command line chatbot implementations](src/test/java/com/datasqrl/ai/example) that demonstrate how to use Acorn Agent generically (i.e. without a framework) at different abstraction levels.

It also includes APIExecutor implementations for GraphQL and REST.

## ConfigurableChatBot

The `ConfigurableChatBot`, found in this [file](src/test/java/com/datasqrl/ai/example/ConfigurableChatBot.java), is an example on how you can create a simple Chatbot application, using the existing ChatProviders for OpenAI, Groq, Vertex, etc.
In order to start the ChatBot, you need to perform a few configuration steps:
1. Define a system prompt
```java
String systemPrompt = "You are a huge fan of the Ricky and Morty TV show and...";
```
2. Load the tools from a json file:
```java
URL toolsResource = ConfigurableChatBot.class.getClassLoader().getResource("tools/rickandmorty.tools.json");
List<RuntimeFunctionDefinition> tools = ToolsBackendFactory.readTools(toolsResource);
```
3. Specify your ApiExecutor and your ToolsBackend. The ApiExecutor is the API that your tools use and the ToolsBackend is the engine that validates tool calls and executes the tools against the API. In this example, we have chosen a GraphQL API of the Rick & Morty TV Series.
```java
APIExecutor apiExecutor = new GraphQLExecutorFactory().create(new MapConfiguration(Map.of(
        "type", "graphql",
        "url", "https://rickandmortyapi.com/graphql"
    )), "rickandmortyapi");
ToolsBackend toolsBackend = ToolsBackendFactory.of(tools, Map.of(APIExecutorFactory.DEFAULT_NAME, apiExecutor));
```
4. Specify the model configuration in code, and then you get a ChatProvider, enriched with tools, which you can use to communicate with the model.
```java
    ChatProvider<?, ?> chatProvider = ChatProviderFactory.fromConfiguration(Map.of(
        "provider", "groq",
        "name", "mixtral-8x7b-32768",
        "temperature", 0.8
    ), toolsBackend, systemPrompt);
```
Now, you can use the ChatProvider to chat with the LLM:
```java
GenericChatMessage response = chatProvider.chat(message, context);
```
The ChatProvider is an abstraction created to give you a very fast start into a tool-enabled Agent.
Although not apparent in the code, in this example, the ChatProvider keeps the message history in memory and feeds it to the LLM as part of the context window on every query. If you want to control this behavior, check out the LowLevelAgent below.

## LowLevelAgent

The `LowLevelAgent`, found in this [file](src/test/java/com/datasqrl/ai/example/LowLevelAgent.java), is an example on how you can control all the fine details of your interactions with the LLM Agent.
For this Example, we use a REST API and a model from OpenAI. The initial configuration is more or less the same as before: 
```java
String systemPrompt = "You are an incredibly enthusiastic and joyous life coach...";
URL toolsResource = LowLevelAgent.class.getClassLoader().getResource("tools/advice.tools.json");
APIExecutor apiExecutor = new RESTExecutorFactory().create(new MapConfiguration(Map.of(
    "type", "rest",
    "url", "https://api.adviceslip.com"
)), "adviceapi");
List<RuntimeFunctionDefinition> tools = ToolsBackendFactory.readTools(toolsResource);
ToolsBackend toolsBackend = ToolsBackendFactory.of(tools, Map.of(APIExecutorFactory.DEFAULT_NAME, apiExecutor));
String openaiKey = ConfigurationUtil.getEnvOrSystemVariable("OPENAI_API_KEY");
OpenAIModelConfiguration chatConfig = new OpenAIModelConfiguration(
    new MapConfiguration(Map.of(
        "name", "gpt-4o-mini",
        "temperature", 0.3)));
```
However, the actual Agent implementation uses the `openai4j` library to talk to the model and accesses the `ChatSession` directly.
It also validates the function calls and executes them with the help of the `ChatSession`.
It's actually quite close to the implementation of the `OpenAiChatProvider`, which can be configured as seen in the [ConfigurableChatBot](src/test/java/com/datasqrl/ai/example/ConfigurableChatBot.java) and [AcornAgentServer](/acorn-spring/src/main/java/com/datasqrl/ai/spring/AcornAgentServer.java) to abstract away all the implementation details.

But this example shows the details, so we have inherited the `ChatSession` class, in order to override the `getContextWindow()` method:
```java
    // Custom implementation of a ChatSession method in order to control the size of the context window, which can be costly
    @Override
    protected ContextWindow<GenericChatMessage> getContextWindow(int maxTokens, ModelAnalyzer<ChatMessage> analyzer) {
      GenericChatMessage systemMessage = this.bindings.convertMessage(this.bindings.createSystemMessage(systemPrompt), this.context);
      final AtomicInteger numTokens = new AtomicInteger(0);
      //      Count tokens for system prompt
      numTokens.addAndGet(systemMessage.getNumTokens());
      ContextWindow.ContextWindowBuilder<GenericChatMessage> builder = ContextWindow.builder();
      //      Count tokens for functions
      this.backend.getFunctions().values().stream()
          .map(RuntimeFunctionDefinition::getChatFunction)
          .peek(f -> numTokens.addAndGet(analyzer.countTokens(f)))
          .forEach(builder::function);
      if (numTokens.get() > maxTokens)
        throw new IllegalArgumentException("Function calls and system message too large for model: " + numTokens);
      int numMessages = messages.size();
      List<GenericChatMessage> contextMessages = new ArrayList<>();
      ListIterator<GenericChatMessage> listIterator = messages.listIterator(numMessages);
      //      Allow maximum 3 past messages in context window
      while (listIterator.hasPrevious() && contextMessages.size() < 3) {
        GenericChatMessage message = listIterator.previous();
        numTokens.addAndGet(message.getNumTokens(msg -> analyzer.countTokens(bindings.convertMessage(msg))));
        if (numTokens.get() > maxTokens) break;
        contextMessages.add(message);
        numMessages--;
      }
      builder.message(systemMessage);
      Collections.reverse(contextMessages);
      builder.messages(contextMessages);
      builder.numTokens(numTokens.get());
      ContextWindow<GenericChatMessage> window = builder.build();
      if (numMessages > 0) log.info("Truncated the first {} messages", numMessages);
      return window;
    }
```
Although quite lengthy, the method just counts the tokens of the prompt, the tools and the messages in the history.
It sets an arbitrary limit of maximum 3 messages from the past to save in the context window.
This is an example on how you can keep the size of your paid tokens in check.

Note that a message can be a user message, a model message, a function call or function response. 
