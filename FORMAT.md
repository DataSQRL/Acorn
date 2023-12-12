# SmartRAG Format

The SmartRAG configuration is a JSON file that defines the set of functions exposed to the LLM and how they map to API queries. The configuration format extends [OpenAI's function call](https://platform.openai.com/docs/guides/function-calling) configuration by introducing the following additional fields (in addition to `type` and `function`):

* `context`: An array of field names that are provided by the context in which this function is executed. These fields are filtered out of the function definition before the definition is passed to the LLM and automatically filled in before the corresponding API query is executed.
* `api`: Defines the API query that executes the function and returns the requested data. Currently, the `api` config object has a single field `query` that defines the GraphQL query.

## Special Functions

SmartRAG defines the following special functions which are used internally and not passed to the LLM:

* `_saveChatMessage`: The API query to store a chat message. This is used to persist the chat history in the API for future retrieval and/or analysis. A chat message has the following fields:
  * `role`: the role of the message
  * `content`: content of the message
  * `name`: name of a function call (only for function invocations)
  * `context`: an object that contains the context
* `_getChatMessages`: The API query to retrieve the chat message history to continue a conversation. Accepts context fields to retrieve the chat history for a particular user only.



## Examples

You can find example configuration files in the `api-examples` subdirectories.