# Tools Format

The tools configuration is a JSON file that defines the set of functions exposed to the LLM and how they map to API queries. The configuration format extends the popular tooling format for LLMs by introducing the following additional fields (in addition to `type` and `function`):

```json
[
  {
    "type": "api",
    "function": {
      "name": "ordered_products",
      "description": "Returns products the customer previously purchased decreasing order by most purchased. Use to recommend products for the customer to purchase.",
      "parameters": {
        "type": "object",
        "properties": {
          "customerid": {
            "type": "integer"
          }
        },
        "required": ["customerid"]
      }
    },
    "context": ["customerid"],
    "api": {
      "query": "query OrderAgainProducts($customerid: Int!) {\nOrderAgain(customerid: $customerid) {\n  product {\n    id\n    name\n    category\n    type\n    sizing\n  }\n  num\n  quantity\n}\n}"
    }
  },
  {
    "type": "api",
    "function": {
      "name": "weather_forecast",
      "description": "Look up weather information for the past 1 and next 12 hours.",
      "parameters": {
        "type": "object",
        "properties": {
          "latitude": {
            "type": "number",
            "description": "The latitude of the location as a decimal number."
          },
          "longitude": {
            "type": "number",
            "description": "The longitude of the location as a decimal number."
          },
          "measurement": {
            "type": "string",
            "description": "The type of weather measurement to look up.",
            "enum": ["temperature_2m","wind_speed_10m","precipitation_probability","precipitation","cloudcover"]
          }
        },
        "required": ["latitude","longitude","measurement"]
      }
    },
    "context": [],
    "api": {
      "path": "/forecast?latitude={latitude}&longitude={longitude}&hourly={measurement}&past_hours=1&forecast_hours=12",
      "method": "GET"
    }
  }
]
```


* `type`: The type of function: `api` (executed against an API), `local` (executed locally), or `client` (send as a callback to the client for execution).
* `context`: An array of field names that are provided by the context in which this function is executed. These fields are filtered out of the function definition before the definition is passed to the LLM and automatically filled in before the corresponding API query is executed.
* `api`: Defines the API query that executes the function and returns the requested data. The `api` config object has the following fields:
  * `name`: Name of the API which must match a configured APIExecutor. Defaults to `default`. Optional
  * `query`: Query to execute (for GraphQL and JDBC) with parameters that match the function parameters. Only include for GraphQL and JDBC. 
  * `path` and `method`: The REST method (GET, POST, etc) and path for the REST resource. The path can contain path arguments that match the function arguments. Only include for REST.

## Special Functions

Acorn Agent defines the following special functions which are used internally against a GraphQL API for message persistence and not passed to the LLM:

* `InternalSaveChatMessage`: The API query to store a chat message. This is used to persist the chat history in the API for future retrieval and/or analysis. A chat message has the following fields:
  * `role`: the role of the message
  * `content`: content of the message
  * `function_call`: the function call, used by an LLM to invoke a function (only for function invocations)
  * `name`: name of a function call (only for function invocations)
  * `[context_fields]`: fields for the context
* `InternalGetChatMessages`: The API query to retrieve the chat message history to continue a conversation. Accepts context fields to retrieve the chat history for a particular user only.

```json
[
  {
    "type": "api",
    "function": {
      "name": "InternalSaveChatMessage",
      "description": "Saves User Chat Messages",
      "parameters": {
        "type": "object",
        "properties": {
        },
        "required": []
      }
    },
    "context": ["customerid"],
    "api": {
      "query": "mutation AddChatMsg($role: String!, $content: String!, $name: String, $functionCall: String, $customerid: Int!) {\n  AddChatMessage(message: {role: $role, content:$content, name: $name, functionCall: $functionCall, customerid: $customerid}) {\n    _source_time\n  }\n}"
    }
  },
  {
    "type": "api",
    "function": {
      "name": "InternalGetChatMessages",
      "description": "Retrieves User Chat Messages",
      "parameters": {
        "type": "object",
        "properties": {
        },
        "required": []
      }
    },
    "context": ["customerid"],
    "api": {
      "query": "query GetChatMessages($customerid: Int!, $limit: Int = 10) {\nmessages: CustomerChatMessage(customerid: $customerid, limit:$limit) {\n  role\n  content\n  functionCall\n  name\n}\n}"
    }
  }
]
```

## Examples

You can find more example configuration files in the [examples](examples/) directory or [test cases](java/acorn-core/src/test/resources/).