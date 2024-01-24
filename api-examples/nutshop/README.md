# Nutshop API

This is an example Customer 360 API that exposes information about a customer's orders and spending.

## 1. Run the API

To run this example, invoke the following command in this directory on Unix based systems
```bash
docker run -it -p 8888:8888 -p 8081:8081 -v $PWD:/build datasqrl/cmd run nutshop-c360.sqrl nutshop-c360.graphqls
```

If you are on windows using Powershell, run the following:
```bash
docker run -it -p 8888:8888 -p 8081:8081 -v ${PWD}:/build datasqrl/cmd run nutshop-c360.sqrl nutshop-c360.graphqls
```

This command stands up the API using [DataSQRL](https://www.datasqrl.com/), a development tool
for data pipelines. To check that the GraphQL API is running properly, [open GraphiQL](http://localhost:8888/graphiql/) to access the API.

## 2. Run the ChatBot

Run the ChatBot in the language of your choice. Check the particular language implementation for details (e.g. [Java](../../java/)).

Provide the name of this example `nutshop` as the command line argument.
You need to enter a customer id (to "log in" as the respective customer), pick a number between 1-9.
