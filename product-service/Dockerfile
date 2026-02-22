FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 9003
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:9003/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
