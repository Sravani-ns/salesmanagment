# Stage 1: Build the application
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# Copy Gradle files to cache dependencies
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code and build
COPY src ./src
RUN ./gradlew build -x test --no-daemon

# Stage 2: Create runtime image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /app/build/libs/salesmanagement-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]