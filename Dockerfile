# Stage 1 – Frontend build
FROM node:lts-alpine AS frontend
WORKDIR /app/komga-webui
COPY komga-webui/package*.json ./
RUN npm install
COPY komga-webui/ ./
RUN npm run build

# Stage 2 – Backend build
FROM eclipse-temurin:21-jdk-alpine AS backend
WORKDIR /build

# Copy Gradle wrapper and build files first for layer caching
COPY gradlew gradlew.bat gradle.properties settings.gradle ./
COPY gradle/ gradle/
COPY build.gradle.kts ./
COPY komga/build.gradle.kts komga/
COPY komga-tray/build.gradle.kts komga-tray/

# Copy the rest of the backend source
COPY komga/ komga/
COPY res/ res/

# Embed the built frontend into the Spring Boot static resources directory
COPY --from=frontend /app/komga-webui/dist/ komga/src/main/resources/public/

# Build the executable jar with embedded frontend
RUN ./gradlew :komga:bootJar --no-daemon

# Stage 3 – Runtime image
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

COPY --from=backend /build/komga/build/libs/komga-*.jar app.jar

VOLUME ["/config", "/data"]
ENV KOMGA_CONFIGDIR="/config"

EXPOSE 25600

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
