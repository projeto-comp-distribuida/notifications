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

# Remove native libraries (.so, .dylib, .dll) to force pure Java mode
# Process nested JARs sequentially to reduce memory usage
RUN apk add --no-cache zip unzip && \
    TEMP_DIR=$(mktemp -d) && \
    MAIN_TEMP=$(mktemp -d) && \
    # Extract main JAR
    unzip -q /app/app.jar -d "$MAIN_TEMP" && \
    # Remove native libraries from main JAR contents
    find "$MAIN_TEMP" -type f \( -name "*.so" -o -name "*.dylib" -o -name "*.dll" \) -delete 2>/dev/null || true && \
    # Process nested JARs one at a time to minimize memory usage
    if [ -d "$MAIN_TEMP/BOOT-INF/lib" ]; then \
        for nested_jar in "$MAIN_TEMP"/BOOT-INF/lib/*.jar; do \
            if [ -f "$nested_jar" ]; then \
                NESTED_TEMP=$(mktemp -d) && \
                unzip -q "$nested_jar" -d "$NESTED_TEMP" 2>/dev/null && \
                find "$NESTED_TEMP" -type f \( -name "*.so" -o -name "*.dylib" -o -name "*.dll" \) -delete 2>/dev/null || true && \
                cd "$NESTED_TEMP" && \
                zip -q -r "$nested_jar.new" . && \
                mv "$nested_jar.new" "$nested_jar" && \
                rm -rf "$NESTED_TEMP"; \
            fi; \
        done; \
    fi && \
    # Repackage the main JAR
    cd "$MAIN_TEMP" && \
    zip -q -r /app/app.jar.new . && \
    mv /app/app.jar.new /app/app.jar && \
    rm -rf "$MAIN_TEMP" "$TEMP_DIR" && \
    apk del zip unzip

# Create non-root user
RUN addgroup --system app && \
    adduser -S -s /bin/false -G app app && \
    chown -R app:app /app

# Force Snappy to use pure Java mode
ENV JAVA_TOOL_OPTIONS="-Dorg.xerial.snappy.purejava=true"

USER app
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]