# Multi-stage Dockerfile for DistriSchool Microservice
# Supports development (hot reload) and optimized production builds

# Stage 1: Cache dependencies
FROM maven:3.9-eclipse-temurin-17-alpine AS deps
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve

# Stage 2: Build application
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY --from=deps /root/.m2/repository /root/.m2/repository
COPY . .
RUN mvn package -DskipTests

# Stage 3: Development (hot reload)
FROM maven:3.9-eclipse-temurin-17-alpine AS dev
WORKDIR /app
COPY --from=deps /root/.m2/repository /root/.m2/repository
COPY ./docker-entrypoint.sh /docker-entrypoint.sh

# Install tools for hot reload and native library support
RUN apk add --no-cache inotify-tools wget ca-certificates && \
    wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.35-r1/glibc-2.35-r1.apk && \
    apk add --no-cache glibc-2.35-r1.apk && \
    rm -f glibc-2.35-r1.apk && \
    chmod +x /docker-entrypoint.sh

ENV SPRING_DEVTOOLS_RESTART_ENABLED=true \
    SPRING_DEVTOOLS_LIVERELOAD_ENABLED=true
EXPOSE 8080 5005 35729
ENTRYPOINT ["/docker-entrypoint.sh"]

# Stage 4: Production
FROM eclipse-temurin:17-jdk-alpine AS release
WORKDIR /app

# Install glibc for native libraries
RUN apk add --no-cache wget ca-certificates && \
    wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.35-r1/glibc-2.35-r1.apk && \
    apk add --no-cache glibc-2.35-r1.apk && \
    rm -f glibc-2.35-r1.apk

# Copy built JAR
COPY --from=build /app/target/microservice-template-1.0.0.jar /app/app.jar

# Force Snappy to use pure Java mode (no native libraries)
# Snappy is included for decompression only (to handle Snappy-compressed messages from Kafka)
# We don't compress with Snappy, but we need it to decompress existing messages
ENV JAVA_TOOL_OPTIONS="-Dorg.xerial.snappy.purejava=true"

# Create non-root user
RUN addgroup --system app && \
    adduser -S -s /bin/false -G app app && \
    chown -R app:app /app

USER app
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]