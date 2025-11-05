# Multi-stage Dockerfile para DistriSchool Microservice Template
# Suporta desenvolvimento com hot reloading e produção otimizada

# Stage 1: Resolver dependências (cache layer)
FROM maven:3.9-eclipse-temurin-17-alpine AS deps

WORKDIR /app
COPY pom.xml /app

# Resolve dependências Maven
RUN mvn dependency:resolve

# Stage 2: Build da aplicação
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app
COPY --from=deps /root/.m2/repository /root/.m2/repository
COPY . /app

RUN mvn package -DskipTests

# Stage 3: Desenvolvimento com hot reloading
FROM maven:3.9-eclipse-temurin-17-alpine AS dev

WORKDIR /app
COPY --from=deps /root/.m2/repository /root/.m2/repository
COPY ./docker-entrypoint.sh /docker-entrypoint.sh

# Instala dependências necessárias
# inotify-tools para monitorar mudanças nos arquivos
# glibc e compat para suporte a bibliotecas nativas como Snappy
RUN apk add --no-cache inotify-tools && \
    apk add --no-cache --virtual .build-deps wget ca-certificates && \
    wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.35-r1/glibc-2.35-r1.apk && \
    apk add --no-cache glibc-2.35-r1.apk && \
    apk del .build-deps && \
    rm -f glibc-2.35-r1.apk
RUN chmod +x /docker-entrypoint.sh

# Configurações para hot reload
ENV SPRING_DEVTOOLS_RESTART_ENABLED=true
ENV SPRING_DEVTOOLS_LIVERELOAD_ENABLED=true
ENV SPRING_DEVTOOLS_RESTART_POLL_INTERVAL=1000
ENV SPRING_DEVTOOLS_RESTART_QUIET_PERIOD=400

EXPOSE 8080 5005 35729

ENTRYPOINT ["/docker-entrypoint.sh"]

# Stage 4: Produção otimizada
FROM eclipse-temurin:17-jdk-alpine AS release

LABEL maintainer="DistriSchool Team"
WORKDIR /app

# Instala glibc para suporte a bibliotecas nativas como Snappy
RUN apk add --no-cache --virtual .build-deps wget ca-certificates && \
    wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.35-r1/glibc-2.35-r1.apk && \
    apk add --no-cache glibc-2.35-r1.apk && \
    apk del .build-deps && \
    rm -f glibc-2.35-r1.apk

# Copia o JAR construído
COPY --from=build /app/target/microservice-template-1.0.0.jar /app/app.jar

# Cria usuário não-root para segurança
RUN addgroup --system app && adduser -S -s /bin/false -G app app
RUN chown -R app:app /app

# Create wrapper script to ensure Snappy pure Java mode is set before JVM starts
RUN echo '#!/bin/sh' > /app/run.sh && \
    echo 'export JAVA_TOOL_OPTIONS="-Dorg.xerial.snappy.purejava=true ${JAVA_TOOL_OPTIONS}"' >> /app/run.sh && \
    echo 'exec java -Dorg.xerial.snappy.purejava=true -jar /app/app.jar "$@"' >> /app/run.sh && \
    chmod +x /app/run.sh && \
    chown app:app /app/run.sh

# Set environment variable to force pure Java Snappy
ENV JAVA_TOOL_OPTIONS="-Dorg.xerial.snappy.purejava=true"
ENV ORG_XERIAL_SNAPPY_PUREJAVA=true

USER app

EXPOSE 8080

# Use wrapper script to ensure Snappy pure Java mode
CMD ["/app/run.sh"]