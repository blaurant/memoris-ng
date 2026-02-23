# --- Build stage ---
FROM clojure:temurin-21-tools-deps AS builder
WORKDIR /app
COPY deps.edn build.clj ./
RUN clojure -P && clojure -P -T:build
COPY . .
RUN clojure -T:build uber

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/app.jar app.jar
EXPOSE 8080

# No APP_ENV default: system.clj will fail-fast in deployed environments
ENTRYPOINT ["java", "-jar", "app.jar"]
