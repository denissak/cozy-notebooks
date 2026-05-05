# syntax=docker/dockerfile:1.7

# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradle ./gradle
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
RUN chmod +x ./gradlew && ./gradlew --version

COPY src ./src

RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as non-root user
RUN useradd -r -u 1001 -g root cozy && chown -R cozy:root /app
USER 1001

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
