@echo off
REM Compile Tessera and package a runnable jar into dist\Tessera.jar.
REM Requires a JDK 21 (or newer) with javac and jar on PATH.
setlocal

cd /d "%~dp0"

if exist bin rmdir /s /q bin
if exist dist rmdir /s /q dist
mkdir bin
mkdir dist

echo Compiling sources...
dir /s /b src\*.java > "%TEMP%\tessera_sources.txt"
javac -d bin @"%TEMP%\tessera_sources.txt"
if errorlevel 1 (
    echo Compile failed.
    exit /b 1
)

echo Packaging Tessera.jar...
jar --create --file dist\Tessera.jar --main-class tessera.Tessera -C bin .
if errorlevel 1 (
    echo Jar packaging failed.
    exit /b 1
)

echo Done: dist\Tessera.jar
echo Run it with:  java -jar dist\Tessera.jar   (or double-click the jar)
endlocal
