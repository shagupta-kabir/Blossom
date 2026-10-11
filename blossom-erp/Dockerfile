# ---- build stage: Maven 3.9 on Amazon Corretto 25 ----
FROM maven:3.9-amazoncorretto-25-al2023 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
# Tests run in CI (GitHub Actions) / locally with `mvn verify`; skipped here to keep deploys fast.
RUN mvn -B -q clean package -DskipTests

# ---- runtime stage: Amazon Corretto 25 ----
FROM amazoncorretto:25
WORKDIR /app
COPY --from=build /workspace/target/blossom-erp.jar app.jar
USER 10001:10001
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
# Render injects PORT; application.yml reads it (default 8080)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
