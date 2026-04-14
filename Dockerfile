# Build stage
FROM maven:3.9.5-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Environment variables
ENV SERVER_PORT=8082
ENV SPRING_PROFILES_ACTIVE=prod
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
ENV MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true

# Expose the port the app runs on
EXPOSE 8082

# --- HEALTHCHECK Addition ---
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health/liveness || exit 1

# Command to run the application
ENTRYPOINT ["java", "-Dserver.port=8082", "-jar", "app.jar"]