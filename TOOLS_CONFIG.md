# Tools Format

The tools configuration is a JSON file that defines the set of functions exposed to the LLM and how they map to API queries. The configuration format extends the popular tooling format for LLMs by introducing the following additional fields (in addition to `type` and `function`):

* `type`: The type of function: `api` (executed against an API), `local` (executed locally), or `client` (send as a callback to the client).
* `context`: An array of field names that are provided by the context in which this function is executed. These fields are filtered out of the function definition before the definition is passed to the LLM and automatically filled in before the corresponding API query is executed.
* `api`: Defines the API query that executes the function and returns the requested data. The `api` config object has the following fields:
  * `name`: Name of the API which must match a configured APIExecutor. Defaults to `default`. Optional
  * `query`: Query to execute (for GraphQL and JDBC) with parameters that match the function parameters. Only include for GraphQL and JDBC. 
  * `path` and `method`: The REST method (GET, POST, etc) and path for the REST resource. The path can contain path arguments that match the function arguments. Only include for REST.

## Special Functions

Acorn Agent defines the following special functions which are used internally for message persistence and not passed to the LLM:

* `InternalSaveChatMessage`: The API query to store a chat message. This is used to persist the chat history in the API for future retrieval and/or analysis. A chat message has the following fields:
  * `role`: the role of the message
  * `content`: content of the message
  * `function_call`: the function call, used by an LLM to invoke a function (only for function invocations)
  * `name`: name of a function call (only for function invocations)
  * `[context_fields]`: fields for the context
* `InternalGetChatMessages`: The API query to retrieve the chat message history to continue a conversation. Accepts context fields to retrieve the chat history for a particular user only.

## Examples

You can find example configuration files in the [examples](examples/) directory.