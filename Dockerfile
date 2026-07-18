# Use an OpenJDK image as the base image
FROM openjdk:21-ea-17-jdk-oracle

# Set the working directory in the container
WORKDIR /app

# Copy the Gradle build file and source code
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
COPY src ./src

# Install dependencies and build the project
RUN ./gradlew installShadowDist

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD pgrep -f "fluminense-telegram-bot" || exit 1

# Set the entry point
CMD ["./build/install/fluminense-telegram-bot-shadow/bin/fluminense-telegram-bot"]
