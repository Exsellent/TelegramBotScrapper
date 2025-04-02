FROM openjdk:23-jdk-slim
COPY bot/target/link-tracker-bot.jar /app/link-tracker-bot.jar
COPY scrapper/target/scrapper.jar /app/scrapper.jar
WORKDIR /app
CMD ["java", "-jar", "link-tracker-bot.jar"]
