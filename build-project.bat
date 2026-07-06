@echo off  
set "PATH=C:\\tools\\node;C:\\tools\\jdk\\bin;C:\\tools\\conveyor\\bin;%PATH%"  
set "CONVEYOR_AGREE_TO_LICENSE=1"
set NODE\_OPTIONS=--max-old-space-size=4096

if exist "%~dp0backend\_build.log" del "%~dp0backend\_build.log"  
if exist "%~dp0komga\\build\\libs\*.jar" del "%~dp0komga\\build\\libs\*.jar"

echo \[1/3\] Building Frontend and Backend via Official Gradle Route...  
cd /d "%~dp0"

:: Execute official Komga tasks to compile and bundle frontend properly into the template classpath  
call gradlew.bat :komga:prepareThymeLeaf :komga:bootJar --no-daemon --console=plain -Dorg.gradle.jvmargs="-Xmx2560m" -PgitProperties.failOnNoGitDirectory=false 2>&1 | powershell -Command "$Input | Tee-Object -FilePath '%~dp0backend_build.log'"

if %ERRORLEVEL% neq 0 (  
echo ERROR: Gradle build failed. Please check backend\_build.log  
exit /b %ERRORLEVEL%  
)

echo \[2/3\] Packaging Windows Desktop Application (.exe)...  
:: Execute Conveyor to compile komga-tray and output the standalone exe  
call conveyor.exe make unzipped-wm-dir  
if %ERRORLEVEL% neq 0 (  
echo ERROR: Conveyor packaging failed.  
exit /b %ERRORLEVEL%  
)

echo \[3/3\] Building Docker image...  
docker build -f Dockerfile.local -t yamada-tech/komga:latest .  
if %ERRORLEVEL% neq 0 (  
echo ERROR: Docker build failed.  
exit /b %ERRORLEVEL%  
)

echo Build process completed successfully.
