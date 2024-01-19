# Java Implementation of apiRAG

This is the Java implementation of apiRAG.

It is based on the [OpenAI Java library](https://github.com/TheoKanning/openai-java) by Theo Kanning.

You can run the examples in one of two ways:
1. Using the [`CmdLineChatBot`](test/java/com/datasqrl/ai/CmdLineChatBot.java) which is a simple ChatBot command line application
2. Using the [`SimpleServer`](test/java/com/datasqrl/ai/spring/SimpleServer.java) which provides the ChatBot as a Spring Boot application exposed through REST and can be accessed via the frontend defined in `../html/chatbot.html`.

For either one you need:
1. To set `OPENAI_TOKEN` as an environment variable that contains your OPENAI API key. In the terminal, run `export OPENAI_TOKEN={YOUR TOKEN}`.
2. Provide the name of the example you want to run as a command line argument (for the server, provide the argument as `--example=[NAME]`). You can find all example definitions in the [`Examples`](test/java/com/datasqrl/ai/Examples.java) enum. 

This is a proof-of-concept implementation and the library has not yet been published to Maven Central. Test coverage is minimal. We are working on it.