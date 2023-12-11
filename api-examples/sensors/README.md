# Sensor API

This is an example API for recording and analyzing metrics data.
Specifically, this API collects the temperature readings of sensors.

## 1. Run the API

To run this example, invoke the following command in this directory
```bash
docker run -it -p 8888:8888 -p 8081:8081 -v $PWD:/build datasqrl/cmd run sensors.sqrl sensorsapi.graphqls
```

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

You can find the associated ChatBot under the programming language directory of your choice. It is called
`SensorsChatBot`. 
Before you run it, make sure you have exported your OpenAI API key in the environment variable `OPENAI_TOKEN`:
```bash
export OPENAI_TOKEN={YOUR TOKEN}
```

Run the Chatbot on the command line and ask it questions. You can add additional
temperature readings and see how the answers update in realtime.