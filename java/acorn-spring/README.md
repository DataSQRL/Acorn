# Spring Boot

This module contains a generic implementation of a ChatBot as a Spring Boot application with Acorn Agent.

* **[AcornAgentServer](src/main/java/com/datasqrl/ai/spring)**: A spring boot server that reads a model, chat, and tools configuration from file to provide a ChatBot.
* **[API Executors](src/main/java/com/datasqrl/ai/api)**: Spring specific executors for GraphQL, REST, and JDBC.
* **[ChatBot Frontend](src/main/resources/public)**: Javascript implementation of a ChatBot frontend with custom data visualization.