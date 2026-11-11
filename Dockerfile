# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Cache dependencies first
COPY gradlew .
COPY gradle gradle
COPY build.gradle* settings.gradle* ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# Build the app
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Pick the runnable jar (skip the *-plain.jar)
RUN cp "$(ls build/libs/*.jar | grep -v plain | head -n 1)" /workspace/app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/uploads /app/config \
    && chown -R app:app /app

COPY --from=build /workspace/app.jar app.jar

USER app
EXPOSE 8085

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]