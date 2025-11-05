# Multi-stage Dockerfile for DistriSchool Microservice
# Supports development (hot reload) and optimized production builds

# Stage 1: Cache dependencies (glibc-based)
FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve

# Stage 2: Build application (glibc-based)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY --from=deps /root/.m2/repository /root/.m2/repository
COPY . .
RUN mvn package -DskipTests

# Stage 3: Development (hot reload) (glibc-based)
FROM maven:3.9-eclipse-temurin-17 AS dev
WORKDIR /app
COPY --from=deps /root/.m2/repository /root/.m2/repository
COPY ./docker-entrypoint.sh /docker-entrypoint.sh

# Install tools for hot reload
RUN apt-get update -y && \
    apt-get install -y --no-install-recommends inotify-tools ca-certificates && \
    rm -rf /var/lib/apt/lists/* && \
    chmod +x /docker-entrypoint.sh

ENV SPRING_DEVTOOLS_RESTART_ENABLED=true \
    SPRING_DEVTOOLS_LIVERELOAD_ENABLED=true
EXPOSE 8080 5005 35729
ENTRYPOINT ["/docker-entrypoint.sh"]

# Stage 4: Production (glibc-based)
FROM eclipse-temurin:17-jre-jammy AS release
WORKDIR /app

# Copy built JAR
COPY --from=build /app/target/microservice-template-1.0.0.jar /app/app.jar

# Force Snappy to use pure Java mode (no native libraries)
# Snappy is included for decompression only (to handle Snappy-compressed messages from Kafka)
# We don't compress with Snappy, but we need it to decompress existing messages
ENV JAVA_TOOL_OPTIONS="-Dorg.xerial.snappy.purejava=true"

# Create non-root user
RUN groupadd -r app && \
    useradd -r -s /usr/sbin/nologin -g app app && \
    chown -R app:app /app

USER app
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]