@echo off
set "PATH=C:\tools\node;%PATH%"

echo [1/3] Building Frontend assets...
cd webui
call npm install
call npm run build
if %ERRORLEVEL% neq 0 (
    echo ERROR: Frontend build failed.
    exit /b %ERRORLEVEL%
)
cd ..

echo [2/3] Building Backend JAR...
call gradlew.bat bootJar --no-daemon -Dorg.gradle.jvmargs="-Xmx1536m" -PgitProperties.failOnNoGitDirectory=false
if %ERRORLEVEL% neq 0 (
    echo ERROR: Backend JAR build failed.
    exit /b %ERRORLEVEL%
)

echo [3/3] Building Docker image...
docker build -t yamada-tech/komga:latest .
if %ERRORLEVEL% neq 0 (
    echo ERROR: Docker build failed.
    exit /b %ERRORLEVEL%
)

echo Build process completed successfully.
