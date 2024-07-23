# OpenAI Model Provider

Access GPT models on [OpenAI](https://openai.com/) through the OpenAI API.
Contains model and provider configuration classes and an implementation of `ChatProvider` for GPT as an abstraction layer for agent interactions.

## Configuration

To use OpenAI as a model provider, you have to sign up on OpenAI.com and create an API access token.
Add the `OPENAI_API_KEY` as an environment variable.

If you configure the ChatProvider via JSON file, the configuration looks like this:
```json
"model" : {
    "provider" : "openai",
    "name": "gpt-4",
    "temperature": 0.9,
    "max_output_tokens": 512,
    "top_p": 0.8
  }
```
For more details, check out [the configuration documentation](/java/acorn-config/README.md)