@echo off
setlocal Enabledelayedexpansion

echo =========================================
echo  Building Komga Server
echo =========================================

:: Fix specific PATH environment to avoid missing tools
set "JAVA_HOME=C:\tools\jdk"
set "NODE_HOME=C:\tools\node"
set "PATH=C:\tools\jdk\bin;C:\tools\node;C:\Windows\system32;C:\Windows"

:: Check tools existence
if not exist "C:\tools\jdk\bin\java.exe" (
    echo [ERROR] java.exe not found under C:\tools\jdk
    goto ERROR
)
if not exist "C:\tools\node\node.exe" (
    echo [ERROR] node.exe not found under C:\tools\node
    goto ERROR
)

echo [OK] Tools detected.
echo Starting Gradle compilation...
echo.

:: Execute main build tasks without clean to prevent folder lock errors
call gradlew.bat shadowJar createPortableZip
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Build failed. Please check the compilation logs above.
    goto ERROR
)

echo.
echo =========================================
echo  [SUCCESS] Build complete!
echo  Check build\distributions\ folder.
echo =========================================
goto END

:ERROR
echo.
echo [PROCESS STOPPED] Execution terminated with errors.
:END
pause
