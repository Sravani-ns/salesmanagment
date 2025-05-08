FROM openjdk:17-jdk-slim
WORKDIR /app
EXPOSE 8080
COPY build/libs/*.jar salesmanagement.jar
COPY src/main/resources/application.properties /app/application.properties
ENTRYPOINT ["java", "-jar", "salesmanagement.jar", "--spring.config.location=file:/app/application.properties"]