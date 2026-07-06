@echo off
set "PATH=C:\tools\node;C:\tools\jdk\bin;C:\tools\conveyor\bin;%PATH%"
set "CONVEYOR_AGREE_TO_LICENSE=1"
set NODE_OPTIONS=--max-old-space-size=4096

cd /d "%~dp0"

if exist "%~dp0dist" rmdir /s /q "%~dp0dist"
mkdir "%~dp0dist"

if exist "%~dp0backend_build.log" del "%~dp0backend_build.log"

echo [1/3] Building Frontend and Backend via Official Gradle Route...
call gradlew.bat :komga:prepareThymeLeaf :komga:bootJar :komga-tray:jar --no-daemon --console=plain -Dorg.gradle.jvmargs="-Xmx2560m" -PgitProperties.failOnNoGitDirectory=false 2>&1 | powershell -Command "$Input | Tee-Object -FilePath '%~dp0backend_build.log'"

if %ERRORLEVEL% neq 0 (
    echo ERROR: Gradle build failed. Please check backend_build.log
    exit /b %ERRORLEVEL%
)

copy /y "%~dp0komga\build\libs\komga-*.jar" "%~dp0dist\" >nul

echo [2/3] Packaging Windows Desktop Application (.exe) into dist/ folder...
call conveyor.exe make -o "%~dp0dist" windows-app
if %ERRORLEVEL% neq 0 (
    echo ERROR: Conveyor packaging failed.
    exit /b %ERRORLEVEL%
)

echo [3/3] Building Docker image using active workspace context...
docker build -f "%~dp0Dockerfile.local" -t yamada-tech/komga:latest "%~dp0"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Docker build failed.
    exit /b %ERRORLEVEL%
)

echo --------------------------------------------------
echo All artifacts successfully aggregated inside: %~dp0dist\
echo Build process completed successfully.
