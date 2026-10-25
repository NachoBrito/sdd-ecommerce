# Stage 1: Build stage
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

# Copy build configuration first for dependency layer caching
COPY gradlew gradlew.bat settings.gradle build.gradle gradle.properties version.txt ./
COPY gradle/ gradle/

# Cache Gradle dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy configuration and source files
COPY config/ config/
COPY src/ src/

# Build production shadow executable archive
RUN ./gradlew shadowJar --no-daemon -x test

# Stage 2: Minimal runtime stage
FROM eclipse-temurin:25-jre AS runner

# Install curl for healthcheck probe and clean cache
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create dedicated non-root application user and group
RUN groupadd -r appgroup && useradd -r -g appgroup -u 10001 appuser

WORKDIR /app

# Copy built application archive
COPY --from=builder --chown=appuser:appgroup /workspace/build/libs/*-all.jar app.jar

# Enforce least privilege by running as non-root user
USER appuser:appgroup

EXPOSE 8080

# Configure healthcheck probe against /health management endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
