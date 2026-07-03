@echo off
setlocal

echo [1/2] Building backend JAR...
call gradlew.bat bootJar --no-daemon -Dorg.gradle.jvmargs="-Xmx1536m"
if errorlevel 1 (
    echo ERROR: Backend JAR build failed.
    exit /b 1
)

echo [2/2] Building Docker image yamada-tech/komga:latest...
docker build -t yamada-tech/komga:latest .
if errorlevel 1 (
    echo ERROR: Docker image build failed.
    exit /b 1
)

echo Build complete.
endlocal
