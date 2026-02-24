# Dockerfile for MyCycleCoach
# This Dockerfile expects a pre-built JAR file in build/libs/
# Build the JAR first with: ./gradlew build
# For CI/CD: GitHub Actions builds the JAR, then builds this Docker image

# ============================================================================
# RUNTIME STAGE (Development)
# ============================================================================
FROM eclipse-temurin:21-jre AS development

WORKDIR /app

# Copy the pre-built JAR
COPY build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Health check: curl the actuator health endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# ============================================================================
# RUNTIME STAGE (Production)
# ============================================================================
FROM eclipse-temurin:21-jre AS production

WORKDIR /app

# Copy the pre-built JAR
COPY build/libs/*.jar app.jar

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser && \
    chown -R appuser:appuser /app

USER appuser

# Expose the application port
EXPOSE 8080

# Health check: curl the actuator health endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM tuning for containerized environments:
# -XX:+UseG1GC - Use G1 garbage collector optimized for containers
# -XX:MaxRAMPercentage=75 - Use 75% of container memory
# -XX:+UseStringDeduplication - Optimize string memory usage
# -XX:+ParallelRefProcEnabled - Parallel reference processing
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75", \
    "-XX:+UseStringDeduplication", \
    "-XX:+ParallelRefProcEnabled", \
    "-jar", "app.jar"]
