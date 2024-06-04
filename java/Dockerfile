FROM maven:3.8.5-openjdk-17 as builder
WORKDIR /app

COPY pom.xml .
COPY apirag-core apirag-core
COPY apirag-bedrock apirag-bedrock
COPY apirag-groq apirag-groq
COPY apirag-openai apirag-openai
COPY apirag-server apirag-server

RUN mvn clean package -DskipTests

FROM openjdk:17-slim
WORKDIR /app

COPY --from=builder /app/apirag-server/target/*.jar server.jar

COPY server-start.sh /app
RUN chmod +x /app/server-start.sh

ENTRYPOINT ["/app/server-start.sh"]
