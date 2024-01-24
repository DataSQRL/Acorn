# API Examples

This directory contains example API definitions that you can run and/or access as a backend
for the apiRAG ChatBot examples.

* [Finance](finance/): An API that gives customers access to their credit card transactions and spending analysis.
* [Nutshop](nutshop/): A Customer 360 API that provides customer specific information for an online nutshop.
* [Sensors](sensors/): A realtime API for collecting and analyzing sensor information.
* [Rick and Morty](rickandmorty/): A publicly accessible API with information about the show.

All but the `Rick and Morty` example require running the API locally so that it can be accessed by the ChatBot.
Check the respective folder on how to run the example APIs. We are using an open-source tool called [DataSQRL](https://github.com/DataSQRL/sqrl) that makes it easy to build and run data APIs. You only need Docker installed on your machine to run the APIs.

To run the actual ChatBot, check out the individual language implementations for details, e.g. [Java](../java/).