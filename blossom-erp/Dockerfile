
# Build stage
FROM maven:3.9-amazoncorretto-25-al2023 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime stage
FROM amazoncorretto:25
WORKDIR /app

COPY --from=build /workspace/target/blossom-erp.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
