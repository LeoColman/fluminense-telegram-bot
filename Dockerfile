# Use an OpenJDK image as the base image
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the Gradle build file and source code
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
COPY src ./src

# Install dependencies and build the project
RUN ./gradlew installShadowDist

# Set the entry point
CMD ["./build/install/fluminense-telegram-bot-shadow/bin/fluminense-telegram-bot"]
