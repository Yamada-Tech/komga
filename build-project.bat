@echo off
set "PATH=C:\tools\node;C:\tools\jdk\bin;C:\tools\conveyor\bin;%PATH%"
set "CONVEYOR_AGREE_TO_LICENSE=1"
set NODE_OPTIONS=--max-old-space-size=4096

if exist "%~dp0backend_build.log" del "%~dp0backend_build.log"
if exist "%~dp0komga\build\libs\*.jar" del "%~dp0komga\build\libs\*.jar"

echo [1/3] Building Frontend and Backend via Official Gradle Route...
cd /d "%~dp0"

call gradlew.bat :komga:prepareThymeLeaf :komga:bootJar :komga-tray:jar --no-daemon --console=plain -Dorg.gradle.jvmargs="-Xmx2560m" -PgitProperties.failOnNoGitDirectory=false 2>&1 | powershell -Command "$Input | Tee-Object -FilePath '%~dp0backend_build.log'"

if %ERRORLEVEL% neq 0 (
    echo ERROR: Gradle build failed. Please check backend_build.log
    exit /b %ERRORLEVEL%
)

echo [2/3] Packaging Windows Desktop Application (.exe)...
call conveyor.exe make windows-app
if %ERRORLEVEL% neq 0 (
    echo ERROR: Conveyor packaging failed.
    exit /b %ERRORLEVEL%
)

echo [3/3] Building Docker image...
docker build -f Dockerfile.local -t yamada-tech/komga:latest .
if %ERRORLEVEL% neq 0 (
    echo ERROR: Docker build failed.
    exit /b %ERRORLEVEL%
)

echo Build process completed successfully.
