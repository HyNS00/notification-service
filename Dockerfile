# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

RUN chmod +x gradlew && ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
