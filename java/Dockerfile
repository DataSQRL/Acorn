FROM maven:3.8.5-openjdk-17 as builder
WORKDIR /app

COPY pom.xml .
COPY apirag-core apirag-core
COPY apirag-udf apirag-udf
COPY apirag-bedrock apirag-bedrock
COPY apirag-groq apirag-groq
COPY apirag-openai apirag-openai
COPY apirag-rest apirag-rest
COPY apirag-graphql apirag-graphql
COPY apirag-config apirag-config
COPY apirag-spring apirag-spring

RUN mvn clean package -DskipTests

FROM openjdk:17-slim
WORKDIR /app

COPY --from=builder /app/apirag-spring/target/*.jar server.jar

COPY server-start.sh /app
RUN chmod +x /app/server-start.sh

ENTRYPOINT ["/app/server-start.sh"]
