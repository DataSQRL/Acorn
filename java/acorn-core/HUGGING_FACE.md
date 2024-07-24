# Enabling Hugging Face Tokenizers

In order to access some tokenizers from Hugging Face, as a user, you need accept their usage terms. This is verified by an access token you can download from their website.

This is how to do it:

1. Head over to the model tokenizer that you want to use, like https://huggingface.co/meta-llama/Meta-Llama-3-70B-Instruct, log in and accept their terms.
2. Go to https://huggingface.co/settings/tokens and download your access token
3. Add it to your environment as `HF_TOKEN=XXXX`