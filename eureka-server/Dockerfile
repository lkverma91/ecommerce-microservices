# ===========================================
# Eureka Server - Multi-stage Dockerfile
# ===========================================
# Stage 1: Build the JAR (Maven image for build)
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install wget for healthcheck
RUN apk add --no-cache wget

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Create non-root user for security
RUN addgroup -S eureka && adduser -S eureka -G eureka
USER eureka

# Expose Eureka port
EXPOSE 8761

## Health check - Eureka exposes /actuator/health
#HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
#  CMD wget -qO- http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
