# Sensor API

This is an example Customer 360 API that exposes information about a customer's orders and spending.

## 1. Run the API

To run this example, invoke the following command in this directory
```bash
docker run -it -p 8888:8888 -p 8081:8081 -v $PWD:/build datasqrl/cmd run nutshop-c360.sqrl nutshop-c360.graphqls
```

This command stands up the API using [DataSQRL](https://www.datasqrl.com/), a development tool
for data pipelines.

## 2. Run the ChatBot

You can find the associated ChatBot under the programming language directory of your choice. It is called
`NutshopChatBot`. 
Before you run it, make sure you have exported your OpenAI API key in the environment variable `OPENAI_TOKEN`:
```bash
export OPENAI_TOKEN={YOUR TOKEN}
```

Run the Chatbot on the command line and ask it questions. You will first be prompted to enter a customer id (between 1 and 9)
to log in as the respective customer.