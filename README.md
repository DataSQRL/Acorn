# Acorn Agent

Acorn Agent is a simple and flexible framework for building AI Agents, Chat Bots, and generative AI applications on the JVM.

Acorn Agent builds on tooling support that most LLMs provide to seamlessly integrate (semi-)structured data from external data sources like APIs, databases, and generic function calls with generative AI for high quality results against your own data.

[visual of data in - acorn agent in the middle - use cases out (like llamaindex)]

# What can You Build with Acorn Agent?

* Chat Bots that retrieve question-specific information from APIs, databases, and other sources on demand.
* AI frontend applications for GraphQL and REST APIs that customize and visualize responses.
* AI Agents that plan and execute actions against existing APIs.
* Semantic search engines structured, semi-structured, and unstructured data that use LLMs for targeted information retrieval.
* AI powered dashboards
* Agents that extract structured data for planning and actions.

Take a look at the [examples](/api-examples) for agents built with Acorn Agent.

# Why Acorn Agent?

Most agent frameworks are either too complex with lots of connectors, configurations, and options or abstract too much away from the actual LLM calls which doesn't give you enough control to fine-tune and cost-optimize your agents.

We wanted to build an agent framework that makes it easy to get started and experiment with different models but doesn't leave you hanging when you need low-level control to optimize performance and cost. In addition, we wanted an agent framework that automatically benefits from the rapid innovation on LLMs without requiring rewriting your agent.

* Acorn Agent is simple: It manages and invokes tools for LLMs efficiently and safely. That's it. [We believe tools are all you need](#tools-are-all-you-need) to succeed with LLMs. And it future-proofs your agent as LLMs get better and better at using tools.
* Acorn Agent is flexible: you can use the lightweight abstraction layers that Acorn agent provides to get started quickly and swap out models easily, but you can also use the tooling framework with any model client library or model API for full control over each model invocation.

# Acorn Agent Features

* Efficient Data Retrieval
* UI integration through dynamic data visualization or UI updates.
* Pluggable models and model providers: OpenAI, Bedrock, Google Vertex, Groq
* Sandboxing and Safety controls
* Message history for context preservation

# Getting Started with Acorn Agent

Examples for demos and pocs and no-code

-> examples

Spring boot application

-> server module

Custom development framework

-> starter module (rename "api" to "starter")
test case

Low-level control

-> starter module

## How does Acorn Agent Work?

![Diagram of how apiRAG executes user requests](img/apiRAG-diagram.png)

3 types of tools:
- api
- user defined function
- client-call

At it's core, apiRAG is a configuration format that defines a set of LLM functions and how they map to API queries.
It extends OpenAI's function call configuration with additional information on how to execute the function against an existing API.

For more information on apiRAG's configuration format, check out the [format documentation](FORMAT.md).

Each apiRAG language implementation is a mapping of the configuration format to the LLM instrumentation for that language. That means, you retain complete control over how the language model gets configured and executed and only need a very lightweight library for pulling in custom data.

apiRAG also introduces the notion of a "context", such as a user id, which is used to constrain the API calls to retrieve
information that pertains to the given context. That allows you to use apiRAG within an authenticated user session
and allowing the LLM to retrieve user-specific information without any danger of leaking information.

## Acorn Agent Concepts



## Tools Are All You Need

We build Acorn Agent after we had the following "aha" moments:
* Advanced LLMs are better at figuring out what a user is asking for than any augmentation approaches we can build around them.
* LLMs are specifically trained on tool usage which is a great way for LLMs to invoke actions and pull information they need to satisfy a user request.

As LLMs rapidly improve and get better at using tools, we believe that "tooling" will become the primary interface through which LLMs interact with the outside world. They can use tools to retrieve information, trigger actions, present information to the user through visualizations, update the UI, etc. In other words, building GenAI applications comes down to providing LLMs with the best tools for the job.

That's why we build Acorn Agent as a tool-centric framework, because tools are all you need to turn an LLM into a full-featured AI agent.

Building AI applications around tooling is not only simpler (everything is a tool) and more flexible (the interface between the LLM and your agent is just a tool repository), but it also future-proves your GenAI application as LLMs will get better and better at using tools. The last thing you want to do is compete with LLMs on who is smarter in the long-run: your application or the LLM.

## Community

[Join our Slack]() to ask questions or share your feedback.

We welcome community contributions to the project.
Acorn Agent is currently limited to JVM applications. We'd love to bring the idea of a tool-centric agent framework to other languages. If you want to help with a Python or JavaScript implementation, please reach out.

## When Should You NOT Use Acorn Agent

We believe that tooling-centric agent frameworks like Acorn Agent are superior to other approaches of augmenting LLMs with custom data sources like RAG, FLARE, or prompt engineering because LLMs are specifically trained on tool usage and their ability to use tools will dramatically increase over the next few years.

However, there are scenarios where a tool-centric approach is currently not ideal:
* You need to use a model that has not been trained on tool usage (see [above](#acorn-agent-features) which models Acorn Agent already supports). In this case, you have to build instrumentation around the LLM using a complex agent framework.
* You are dealing with entirely unstructured data and vector similarity is your primary method of retrieving augmenting information. In this case, you are better off using a general-purpose semantic search engine.