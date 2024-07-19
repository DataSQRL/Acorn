FROM maven:3.8.5-openjdk-17 as builder
WORKDIR /app

COPY pom.xml .
COPY acorn-core acorn-core
COPY acorn-udf acorn-udf
COPY acorn-bedrock acorn-bedrock
COPY acorn-groq acorn-groq
COPY acorn-openai acorn-openai
COPY acorn-rest acorn-rest
COPY acorn-graphql acorn-graphql
COPY acorn-config acorn-config
COPY acorn-spring acorn-spring

RUN mvn clean package -DskipTests

FROM openjdk:17-slim
WORKDIR /app

COPY --from=builder /app/acorn-spring/target/*.jar server.jar

COPY server-start.sh /app
RUN chmod +x /app/server-start.sh

ENTRYPOINT ["/app/server-start.sh"]
