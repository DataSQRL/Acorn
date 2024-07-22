# OpenAI Model Provider

Access GPT models on [OpenAI](https://openai.com/) through the OpenAI API.
Contains model and provider configuration classes and an implementation of `ChatProvider` for GPT as an abstraction layer for agent interactions.

## Configuration

To use OpenAI as a model provider, you have to sign up on OpenAI.com and create an API access token.
Configure the access token via the `OPENAI_API_KEY` environmental variable.