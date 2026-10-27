@echo off
REM ------------------------------------------------------------------
REM  Snake build script.
REM
REM  --release 8 is mandatory: the "java" on PATH is a JRE 1.8 and can
REM  only load class files with major version 52. The vscode-java
REM  language server has no compliance setting, so it recompiles bin
REM  with JDK 17 (major 61) on every save. That is why the IDE output
REM  dir was moved to bin-ide and this script owns bin.
REM
REM  The source list is written as UTF-8 by PowerShell: cmd's "dir /s /b"
REM  writes it in the OEM code page, and the Chinese path in this
REM  workspace then makes javac fail with MalformedInputException.
REM
REM  NOTE: keep this file ASCII-only. cmd.exe reads .bat with the OEM
REM  code page and non-ASCII bytes break the parser.
REM ------------------------------------------------------------------
setlocal

set ROOT=%~dp0
set OUT=%ROOT%bin
set LIST=%TEMP%\snake_sources.txt

echo [1/3] clean output
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

echo [2/3] compile src -^> bin (--release 8)
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; $files = Get-ChildItem -LiteralPath '%ROOT%src' -Recurse -Filter *.java | ForEach-Object { $_.FullName }; [IO.File]::WriteAllLines('%LIST%', $files, (New-Object System.Text.UTF8Encoding($false)))"
if errorlevel 1 (
    echo FAILED: could not collect source list
    exit /b 1
)
javac --release 8 -encoding UTF-8 -d "%OUT%" @"%LIST%"
if errorlevel 1 (
    del /q "%LIST%"
    echo BUILD FAILED
    exit /b 1
)
del /q "%LIST%"

echo [3/3] verify bytecode version
REM Read the class file header directly: javap is not on PATH here.
for /f %%m in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "$b=[IO.File]::ReadAllBytes('%OUT%\com\snake\App.class'); $b[6]*256 + $b[7]"') do set MAJOR=%%m
echo       com.snake.App major version = %MAJOR%
if not "%MAJOR%"=="52" (
    echo FAILED: bytecode is not 52, Java 8 cannot run it.
    exit /b 1
)

echo OK. run with: java -cp bin com.snake.App
endlocal
