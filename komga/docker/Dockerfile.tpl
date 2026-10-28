# 基本画像：OpenJDK 17
FROM openjdk:17-slim

# Working directory
WORKDIR /app

# Copy application files
COPY komga/src/main/resources/ ./src/main/resources/
COPY komga/src/main/kotlin/ ./src/main/kotlin/
COPY komga/src/test/kotlin/ ./src/test/kotlin/

# Build and run application
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/komga-0.0.1-SNAPSHOT.jar"]
