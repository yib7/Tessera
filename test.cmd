@echo off
REM Compile and run the headless logic tests. Exits non-zero on any failure.
setlocal

cd /d "%~dp0"

set BIN_TEST=bin-test

if exist %BIN_TEST% rmdir /s /q %BIN_TEST%
mkdir %BIN_TEST%

echo Compiling sources and tests...
dir /s /b src\*.java test\*.java > "%TEMP%\tessera_test_sources.txt"
javac -d %BIN_TEST% @"%TEMP%\tessera_test_sources.txt"
if errorlevel 1 (
    echo Compile failed.
    exit /b 1
)

echo Running logic tests...
java -cp %BIN_TEST% tessera.LogicTests
exit /b %errorlevel%
