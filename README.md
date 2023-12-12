# SmartRAG

SmartRAG makes it easy to augment Large Language Models intelligently with custom data provided by an external API.

SmartRAG solves the following use cases for Large Language Models (LLM), Generative AI, and ChatBots:
* Improve the breadth, depth, and accuracy of knowledge to answer user queries intelligently. For example, if you want to connect an LLM to an existing knowledge- or database to look up information as we do in our [Rick and Morty example](api-examples/rickandmorty).
* Access user specific or access controlled information to provided personalized answers in a secure manner. For example, allow the LLM to answer customer questions about their profile and transaction history as we do in our [Nutshop example](api-examples/nutshop).
* Include realtime information that updates frequently. For example, have an LLM analyze realtime sensor information as we do in our [Sensors example](api-examples/sensors).

SmartRAG has the following benefits:
* On-Demand: It allows the LLM to retrieve the data it needs when it needs it.
* Efficient: It benefits from the intelligence of the LLM to retrieve exactly the data it needs. 
* Simplicity: It's essentially just a mapping layer between LLM function calls and API queries.
* Modularity: It separates the LLM instrumentation from the data augmentation providing a separation between "frontend" and "backend".
* Reuse: You can reuse your existing APIs.

Check out the [examples](api-examples) for some example use cases and how they are implemented with SmartRAG.

## How does this compare to RAG?

Currently, the most popular approach for augmenting LLMs with custom data is "Retrieval Augmented Generation". The basic idea behind RAG is to take a user query, retrieve information related to the query (usually by way for a vector embedding and vector search), and the put that information into the context for the LLM.

In other words, RAG is essentially a guess as to what the LLM might need to generate a good answer and stuffing it into the context. Because it relies on text or vector search, it only works effectively for use cases where the user query can be easily mapped to a good retrieval query.
And even for use cases where RAG works reasonably well - such as semantic search, it can be very inefficient. For example, if a user asks to "find all information on the 'nightfall' project from last December" RAG may find information on the project but has no way to limit it to the given timeframe. 

SmartRAG does not suffer from these shortcomings because it relies on the LLM to determine what information it needs and relies on the intelligence of LLMs to translate user queries into efficient and relevant retrieval requests.

## How does SmartRAG Work?

At it's core, SmartRAG is a configuration format that defines a set of LLM functions and how they map to API queries.
It extends OpenAI's function call configuration with additional information on how to execute the function against an existing API.

For more information on SmartRAG's configuration format, check out the [format documentation](FORMAT.md).

Each SmartRAG language implementation is a mapping of the configuration format to the LLM instrumentation for that language. That means, you retain complete control over how the language model gets configured and executed and only need a very lightweight library for pulling in custom data.

SmartRAG also introduces the notion of a "context", such as a user id, which is used to constrain the API calls to retrieve
information that pertains to the given context. That allows you to use SmartRAG within an authenticated user session
and allowing the LLM to retrieve user-specific information without any danger of leaking information.

## Current Limitations

SmartRAG is currently a proof-of-concept to demonstrate the utility and efficiency of the approach.
It is currently limited to OpenAI LLMs, GraphQL APIs, and only has a Java implementation.

We plan to overcome these limitations soon and are working on the following roadmap:
* Support for Python and JavaScript/TypeScript
* Support for REST APIs
* Support for open-source and additional LLMs (like Llama2)
* Support for additional API authentication modes

In addition, we welcome community contributions to the project.