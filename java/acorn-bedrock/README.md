# Bedrock Model Provider

Access models on [AWS Bedrock](https://aws.amazon.com/bedrock/) through the AWS Bedrock SDK.
Contains model and provider configuration classes and an implementation of `ChatProvider` for Bedrock as an abstraction layer for agent interactions.

## Configuration

To use Amazon Bedrock as a model provider, you have to create an AWS account and follow the [instructions on how to enable access to the models](https://docs.aws.amazon.com/bedrock/latest/userguide/getting-started.html).
You will need to add your AWS credentials as environment variables:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN` (if available)

The implementation uses HuggingFace tokenizers to count tokens. Check out the [documentation](/java/acorn-core/HUGGING_FACE.md) about how to enable access to some tokenizers and add the `HF_TOKEN` as an environment variable.
Depending on the model that you want to use, you will need to accept the Hugging Face terms and conditions of these providers:
- https://huggingface.co/meta-llama

You will also need to specify the `region` of your AWS Bedrock project in the model configuration. If you configure the ChatProvider via JSON file, the configuration looks like this:
```json
"model" : {
    "provider" : "bedrock",
    "name": "meta.llama3-70b-instruct-v1:0",
    "region": "us-west-2",
    "temperature": 1.0,
    "max_gen_len": 512
    ""
}
```
Note that Bedrock uses `max_gen_len` instead of `max_output_tokens`, but both configuration keys are supported by the Acorn Configuration. 
For more details, check out [the configuration documentation](/java/acorn-config/README.md)