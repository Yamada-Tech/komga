FROM eclipse-temurin:25-jre AS builder
ARG JAR={{distributionArtifactFile}}
WORKDIR /builder
COPY assembly/${JAR} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# amd64 builder
FROM ubuntu:26.04 AS build-amd64
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:25-jre $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"
RUN apt -y update && \
    apt -y install ca-certificates locales libjxl-dev libheif-dev libwebp-dev wget curl && \
    echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen && \
    locale-gen en_US.UTF-8 && \
    wget "https://github.com/pgaskin/kepubify/releases/latest/download/kepubify-linux-64bit" -O /usr/bin/kepubify && \
    chmod +x /usr/bin/kepubify && \
    apt -y autoremove && rm -rf /var/lib/apt/lists/*
ENV LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:/usr/lib/x86_64-linux-gnu"

# arm64 builder
FROM ubuntu:26.04 AS build-arm64
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:25-jre $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"
RUN apt -y update && \
    apt -y install ca-certificates locales libjxl-dev libheif-dev libwebp-dev wget curl && \
    echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen && \
    locale-gen en_US.UTF-8 && \
    wget "https://github.com/pgaskin/kepubify/releases/latest/download/kepubify-linux-arm64" -O /usr/bin/kepubify && \
    chmod +x /usr/bin/kepubify && \
    apt -y autoremove && rm -rf /var/lib/apt/lists/*
ENV LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:/usr/lib/aarch64-linux-gnu"

# NOTE: the ARM32 (linux/arm/v7) stage was removed. ARM32 is dropped because Java support for ARM32 ended with Java 21
# (see https://komga.org/blog/prepare-v2). JReleaser only creates stages for the platforms listed in build.gradle.kts (buildx.platforms).

FROM build-${TARGETARCH} AS runner
VOLUME /config
WORKDIR /app
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
ENV KOMGA_CONFIGDIR="/config"
ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'
ENTRYPOINT ["java", "-Dspring.profiles.include=docker", "--enable-native-access=ALL-UNNAMED", "-jar", "application.jar", "--spring.config.additional-location=file:/config/"]
EXPOSE 25600
LABEL org.opencontainers.image.source="https://github.com/Yamada-Tech/komga"
