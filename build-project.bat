@echo off
set "PATH=C:\tools\jdk\bin;%PATH%"
set NODE_OPTIONS=--max-old-space-size=4096

if exist "%~dp0backend_build.log" del "%~dp0backend_build.log"
if exist "%~dp0komga\build\libs\*.jar" del "%~dp0komga\build\libs\*.jar"

echo [1/2] Building Frontend and Backend via Official Gradle Route...
cd /d "%~dp0"

call gradlew.bat :komga:prepareThymeLeaf :komga:bootJar --no-daemon --console=plain -Dorg.gradle.jvmargs="-Xmx2560m" -PgitProperties.failOnNoGitDirectory=false > "%~dp0backend_build.log" 2>&1

if %ERRORLEVEL% neq 0 (
    echo ERROR: Gradle build failed. Please check backend_build.log
    exit /b %ERRORLEVEL%
)

echo [2/2] Building Docker image...
docker build -t yamada-tech/komga:latest .
if %ERRORLEVEL% neq 0 (
    echo ERROR: Docker build failed.
    exit /b %ERRORLEVEL%
)

echo Build process completed successfully.
