# Examples

All of these examples use a configurable Acorn Agent Server docker image.

## Custom API Examples

You can run the following examples in Docker by standing up an API and running Acorn Agent as a server in front of it:

* [Customer Credit Card](https://github.com/DataSQRL/datasqrl-examples/tree/main/finance-credit-card-chatbot): Builds a ChatBot that analyzes a customer's credit card transactions to answer questions about spending.
* [IoT Bot](https://github.com/DataSQRL/datasqrl-examples/tree/main/iot-sensor-metrics): A data agent for retrieving and analyzing metrics in realtime.
* [Credit Card Rewards](https://github.com/DataSQRL/datasqrl-examples/tree/main/finance-credit-card-chatbot): An agent that helps customers pick the best credit card to maximize rewards.

For these examples, you will stand up an API locally that Acorn Agents instruments for LLM usage. Hence, these examples are a little more involved to run than the public API examples below. However, because we have control over the API, we can use more of Acorn Agent's feature set like storing and retrieving user messages, adding more complex tools for data retrieval, and custom data visualization.

These examples show you what building a production-grade application with Acorn Agent looks like. 

## Public API Examples

This directory contains examples of using apiRAG with publicly accessible APIs. Hence, you don't need to run an API for these examples to work. To run these examples, all you need is Docker and an API key for the Large-Language-Model provider of your choice. 

Running the examples takes less than a minute, and you can modify them to play around with Acorn Agent.

* [Weather Forecast](weather/): A chatbot that can tell you the current and future weather for any location and visualize it.
* [Rick and Morty](rickandmorty/): A chatbot that answers questions about the show by looking up the information from the public Rick and Morty GraphQL API.
* [I'm Bored](activity/): A chatbot that helps you find things to do by looking up activities from a public REST API by App Brewery.
* [Advice Monster](advice/): A chatbot that dispenses advice retrieved from a REST endpoint.

We want to highlight that the APIs used in the examples above are NOT our own and are operated by other people and organizations for the public to enjoy. We do not endorse those APIs. We merely want to show how easy it is to build intelligent ChatBots that can retrieve information from existing APIs for knowledge augmentation.

## Next Steps

These examples showcase how you can build an agent that accesses APIs for data retrieval and presents the information to the user. 

You can configure your own agent against an existing API or one that you build yourself. This requires no coding.
For complete control for the agent implementation, check out source code implementation and examples contained therein.
