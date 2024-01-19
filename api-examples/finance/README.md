# Finance API

This API gives customers access to their credit card transaction history and spending analysis.

## 1. Run the API

To run this example, invoke the following command in this directory
```bash
docker run -it -p 8888:8888 -p 8081:8081 -v $PWD:/build datasqrl/cmd run creditcard.sqrl creditcard.graphqls
```

This command stands up the API using [DataSQRL](https://www.datasqrl.com/), a development tool
for data pipelines.

## 2. Run the ChatBot

Run the ChatBot in the language of your choice. Check the particular language implementation for details (e.g. [Java](../../java/)).

Provide the name of this example `creditcard` as the command line argument.
You need to enter a customer id (to "log in" as the respective customer), pick a number between 1-9.

If you'd like answers to be provided in visual charts, use the example name `ccvisual`. This only works with the server implementation of apiRAG.
