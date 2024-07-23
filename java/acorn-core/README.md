# Core Implementation

This module contains the core implementation of Acorn Agent, which consists of these components:

* **[Tooling](src/main/java/com/datasqrl/ai/tool)**: Register, manage, and invoke tools for LLMs with `ToolsBackend`.
* **[Models](src/main/java/com/datasqrl/ai/models)**: Configure models and an abstraction layer for interacting with models, so they can be easily swapped out. Use `ChatSession` as a low-level interface that only manages messages and tools or `ChatProvider` which provides a higher level interface for agent interactions.
* **[API](src/main/java/com/datasqrl/ai/api)**: Interfaces for calling APIs as a tool.

Check out the [main documentation](../../README.md#acorn-agent-concepts) for an explanation of the main concepts used in this implementation.