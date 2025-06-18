FROM openjdk:23-jdk-slim
WORKDIR /app
COPY bot/target/link-tracker-bot.jar app.jar
RUN apt-get update && apt-get install -y iputils-ping net-tools telnet netcat-openbsd curl && rm -rf /var/lib/apt/lists/*
CMD ["java", "-jar", "app.jar"]
