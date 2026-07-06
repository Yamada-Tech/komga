@echo off
set "PATH=C:\tools\node;C:\tools\jdk\bin;%PATH%"
set NODE_OPTIONS=--max-old-space-size=4096

cd /d "%~dp0"

if exist "%~dp0complete" rmdir /s /q "%~dp0complete"
mkdir "%~dp0complete"

if exist "%~dp0backend_build.log" del "%~dp0backend_build.log"

echo [1/2] Building Frontend and Backend via Official Gradle Route...
call gradlew.bat :komga:prepareThymeLeaf :komga:bootJar :komga-tray:jar --no-daemon --console=plain -Dorg.gradle.jvmargs="-Xmx2560m" -PgitProperties.failOnNoGitDirectory=false 2>&1 | powershell -Command "$Input | Tee-Object -FilePath '%~dp0backend_build.log'"

if %ERRORLEVEL% neq 0 (
    echo ERROR: Gradle build failed. Please check backend_build.log
    exit /b %ERRORLEVEL%
)

copy /y "%~dp0komga\build\libs\komga-*.jar" "%~dp0complete\" >nul
if exist "%~dp0complete\*plain.jar" del "%~dp0complete\*plain.jar" >nul

setlocal enabledelayedexpansion
for %%f in ("%~dp0complete\komga-*.jar") do (
    set "fname=%%~nf"
    set "ver=!fname:komga-=!"
    ren "%%f" "komga-!ver!-custom.jar"
)

echo Checking local environment for Docker / WSL capabilities...
where docker >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [NOTICE] Docker client CLI not detected on this system.
    goto :SKIP_DOCKER_STAGE
)

docker info >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [NOTICE] Docker Daemon/WSL2 is not active or not running.
    goto :SKIP_DOCKER_STAGE
)

echo [2/2] Building Docker image using active workspace context...
docker build -f Dockerfile.local -t yamada-tech/komga:latest .
if %ERRORLEVEL% neq 0 (
    echo ERROR: Docker build failed.
    exit /b %ERRORLEVEL%
)

echo Exporting Production NAS Container Archive (.tar)...
docker save -o "%~dp0complete\komga-docker-!ver!-custom.tar" yamada-tech/komga:latest
if %ERRORLEVEL% neq 0 (
    echo ERROR: Docker save archive export failed.
    exit /b %ERRORLEVEL%
)
endlocal
goto :BUILD_COMPLETE

:SKIP_DOCKER_STAGE
endlocal
echo --------------------------------------------------
echo [WARNING] Docker stages were safely skipped.
echo Reason: Docker Desktop or WSL2 runtime environment was not found.

:BUILD_COMPLETE
echo --------------------------------------------------
echo SUCCESS: Production components aggregated inside:
echo %~dp0complete\
echo Build pipeline executed perfectly.
