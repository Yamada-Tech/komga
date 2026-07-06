@echo off  
set "PATH=C:\\tools\\node;C:\\tools\\jdk\\bin;C:\\tools\\conveyor\\bin;%PATH%"  
set "CONVEYOR\_AGREE\_TO\_LICENSE=1"  
set NODE\_OPTIONS=--max-old-space-size=4096

:: 1. Micro-manage and pinpoint the active repository workspace root directory  
cd /d "%~dp0"

:: 2. Initialize and clear the centralized artifacts delivery folder  
if exist "%~dp0dist" rmdir /s /q "%~dp0dist"  
mkdir "%~dp0dist"

if exist "%~dp0backend_build.log" del "%~dp0backend_build.log"

echo \[1/3\] Building Frontend and Backend via Official Gradle Route...  
call gradlew.bat :komga:prepareThymeLeaf :komga:bootJar :komga-tray:jar --no-daemon --console=plain -Dorg.gradle.jvmargs="-Xmx2560m" -PgitProperties.failOnNoGitDirectory=false 2>&1 | powershell -Command "$Input | Tee-Object -FilePath '%~dp0backend_build.log'"

if %ERRORLEVEL% neq 0 (  
echo ERROR: Gradle build failed. Please check backend_build.log  
exit /b %ERRORLEVEL%  
)

:: Copy the compiled production fat JAR out into our centralized dist root folder instantly  
copy /y "%~dp0komga\\build\\libs\\komga-\*.jar" "%~dp0dist" >vnu

echo \[2/3\] Packaging Windows Desktop Application (.exe) into dist/ folder...  
:: Execute Conveyor targeting the centralized 'dist' directory via the -o output flag  
call conveyor.exe -o "%~dp0dist" make windows-app  
if %ERRORLEVEL% neq 0 (  
echo ERROR: Conveyor packaging failed.  
exit /b %ERRORLEVEL%  
)

echo \[3/3\] Building Docker image using active workspace context...  
:: Dynamically bind the docker context directly to this self-detected directory path  
docker build -f "%~dp0Dockerfile.local" -t yamada-tech/komga:latest "%~dp0"  
if %ERRORLEVEL% neq 0 (  
echo ERROR: Docker build failed.  
exit /b %ERRORLEVEL%  
)

echo --------------------------------------------------  
echo All artifacts successfully aggregated inside: %~dp0dist  
echo Build process completed successfully.
