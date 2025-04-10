FROM openjdk:23-jdk-slim
ARG JAR_FILE
WORKDIR /app
COPY ${JAR_FILE} /app/app.jar
RUN apt-get update && apt-get install -y iputils-ping net-tools telnet netcat-openbsd curl && rm -rf /var/lib/apt/lists/*
CMD ["java", "-jar", "/app/app.jar"]
