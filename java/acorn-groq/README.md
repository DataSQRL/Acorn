# Groq Model Provider

Access models on [Groq](https://groq.com/) through the OpenAI compatible API.
Contains model and provider configuration classes and an implementation of `ChatProvider` for Groq as an abstraction layer for agent interactions.

## Configuration

To use Groq as a model provider, you have to sign up on groq.com and create an API access token.
Add the `GROQ_API_KEY` as an environment variable.

The implementation uses HuggingFace tokenizers to count tokens. Check out the [documentation](/java/acorn-core/HUGGING_FACE.md) about how to enable access to some tokenizers and add the `HF_TOKEN` as an environment variable.
Depending on the model that you want to use, you will need to accept the Hugging Face terms and conditions of these providers:
- https://huggingface.co/meta-llama
- https://huggingface.co/google
- https://huggingface.co/mistralai
 

If you configure the ChatProvider via JSON file, the configuration looks like this:
```json
"model" : {
    "provider" : "groq",
    "name": "mixtral-8x7b-32768",
    "temperature": 0.9,
    "max_output_tokens": 1024,
    "top_p": 0.8
  }
```
For more details, check out [the configuration documentation](/java/acorn-config/README.md)