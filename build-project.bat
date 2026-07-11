@echo off
set "PATH=C:\tools\node;C:\tools\jdk\bin;%PATH%"
set NODE_OPTIONS=--max-old-space-size=4096

cd /d "%~dp0"

echo [0/2] Cleaning previous build artifacts...
if exist "%~dp0complete" rmdir /s /q "%~dp0complete"
if exist "%~dp0build" rmdir /s /q "%~dp0build"
if exist "%~dp0komga\build" rmdir /s /q "%~dp0komga\build"
if exist "%~dp0komga-tray\build" rmdir /s /q "%~dp0komga-tray\build"
if exist "%~dp0komga-webui\dist" rmdir /s /q "%~dp0komga-webui\dist"

if exist "%~dp0backend_build.log" del "%~dp0backend_build.log"

mkdir "%~dp0complete"

echo [1/2] Building Frontend and Backend via Official Gradle Route...
call gradlew.bat clean :komga:prepareThymeLeaf :komga:bootJar :komga-tray:jar --no-daemon --console=plain -Dorg.gradle.jvmargs="-Xmx2560m" -PgitProperties.failOnNoGitDirectory=false > "%~dp0backend_build.log" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"

if %GRADLE_EXIT% neq 0 goto :HANDLE_GRADLE_FAILURE
goto :GRADLE_OK

:HANDLE_GRADLE_FAILURE
findstr /c:"Cannot find module" "%~dp0backend_build.log" >nul
if errorlevel 1 goto :GRADLE_ABORT

echo [RECOVERY] Frontend dependency inconsistency detected. Reinstalling dependencies...
if exist "%~dp0komga-webui\node_modules" rmdir /s /q "%~dp0komga-webui\node_modules"
call gradlew.bat :komga:npmInstall --rerun-tasks --no-daemon --console=plain >> "%~dp0backend_build.log" 2>&1
if errorlevel 1 goto :GRADLE_RECOVERY_ABORT

echo [RECOVERY] Retrying full build after dependency reinstall...
call gradlew.bat clean :komga:prepareThymeLeaf :komga:bootJar :komga-tray:jar --no-daemon --console=plain -Dorg.gradle.jvmargs="-Xmx2560m" -PgitProperties.failOnNoGitDirectory=false > "%~dp0backend_build.log" 2>&1
set "GRADLE_EXIT=%ERRORLEVEL%"
if %GRADLE_EXIT% neq 0 goto :GRADLE_ABORT

:GRADLE_OK
type "%~dp0backend_build.log"
goto :AFTER_GRADLE_BUILD

:GRADLE_RECOVERY_ABORT
type "%~dp0backend_build.log"
echo ERROR: Dependency recovery failed. Please check backend_build.log
exit /b %ERRORLEVEL%

:GRADLE_ABORT
type "%~dp0backend_build.log"
echo ERROR: Gradle build failed. Please check backend_build.log
exit /b %GRADLE_EXIT%

:AFTER_GRADLE_BUILD

copy /y "%~dp0komga\build\libs\komga-*.jar" "%~dp0complete\" >nul
if exist "%~dp0complete\*plain.jar" del "%~dp0complete\*plain.jar" >nul

setlocal enabledelayedexpansion
set "ver="
for %%f in ("%~dp0complete\komga-*.jar") do (
    set "fname=%%~nf"
    set "ver=!fname:komga-=!"
    ren "%%f" "komga-!ver!-custom.jar"
)

if "!ver!"=="" (
    echo ERROR: Built JAR was not found in komga\build\libs. Stopping pipeline.
    endlocal
    exit /b 1
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
