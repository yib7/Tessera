@echo off
REM Run Tessera, building the jar first if it is missing.
setlocal

cd /d "%~dp0"

if not exist dist\Tessera.jar (
    echo Jar not found, building first...
    call build.cmd
    if errorlevel 1 exit /b 1
)

echo Launching Tessera...
javaw -jar dist\Tessera.jar
endlocal
