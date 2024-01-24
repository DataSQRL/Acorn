# Sensor API

This is an example API for recording and analyzing metrics data.
Specifically, this API collects the temperature readings of sensors.

## 1. Run the API

To run this example, invoke the following command in this directory on Unix based systems
```bash
docker run -it -p 8888:8888 -p 8081:8081 -v $PWD:/build datasqrl/cmd run sensors.sqrl sensorsapi.graphqls
```

If you are on windows using Powershell, run the following:
```bash
docker run -it -p 8888:8888 -p 8081:8081 -v ${PWD}:/build datasqrl/cmd run sensors.sqrl sensorsapi.graphqls
```

This command stands up the API using [DataSQRL](https://www.datasqrl.com/), a development tool
for data pipelines. To check that the GraphQL API is running properly, [open GraphiQL](http://localhost:8888/graphiql/) to access the API.

This command stands up the API using [DataSQRL](https://www.datasqrl.com/), a development tool
for data pipelines.

## 2. Add Temperature Readings

Once the API is up and running, you can access it through GraphiQL, a GraphQL IDE by opening
[http://localhost:8888//graphiql/](http://localhost:8888//graphiql/) in your browser.

Before you run the associated ChatBot, we need to record some sensor data through the API.
You can do that by copy-pasting the following GraphQL mutation query and running it in GraphiQL:
```graphql
mutation AddReading {
  AddReading(metric: {sensorid: 1, temperature: 44.1}) {
    _source_time
  }
}
```

Run the query a few times to add some data. Feel free to change the temperature and sensor id.

## 3. Run the ChatBot

Run the ChatBot in the language of your choice. Check the particular language implementation for details (e.g. [Java](../../java/)).

Provide the name of this example `sensor` as the command line argument.
You can now add the ChatBot questions. You can add additional
temperature readings and see how the answers update in realtime.