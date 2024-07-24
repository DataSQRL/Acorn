# Google Vertex Model Provider

Access LLMs hosted on [Google Vertex AI](https://cloud.google.com/vertex-ai) through the Google Vertex API.
Contains model and provider configuration classes and an implementation of `ChatProvider` for Google Vertex as an abstraction layer for agent interactions.

## Configuration

To use Google Vertex as a model provider, you have to create a Google Cloud account and follow the [instructions on how to enable access to the models](https://cloud.google.com/vertex-ai/docs/start/cloud-environment).
You will need to authenticate in your environment before you start an application that accesses the Vertex models. The easiest way to do this is to set up [Application Default Credentials](https://cloud.google.com/docs/authentication/provide-credentials-adc) via the gcloud CLI in your terminal.
```
gcloud auth application-default login
```

You will also need to specify the `project_id` and `location` of your Google Vertex project in the model configuration.
Google Vertex also allows you to set the optional config parameter `top_k`. See [here](https://firebase.google.com/docs/vertex-ai/model-parameters?platform=android#top-k) for details.

If you configure the ChatProvider via JSON file, the configuration looks like this:
```json
"model" : {
    "provider" : "vertex",
    "name": "gemini-1.5-flash",
    "project_id": "vertex-gemini-12433",
    "location": "us-east1",
    "temperature": 0.8,
    "top_k": 40
}
```
For more details, check out [the configuration documentation](/java/acorn-config/README.md)