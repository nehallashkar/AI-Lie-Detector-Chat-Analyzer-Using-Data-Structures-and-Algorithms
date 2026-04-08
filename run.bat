@echo off
title AI Lie Detector Chat Analyzer - Build & Run
color 0B

echo.
echo  =====================================================
echo   AI Lie Detector Chat Analyzer - DSA Project
echo  =====================================================
echo.

:: Check Java is installed
java -version 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not in PATH.
    echo Please install Java 11 or higher from https://adoptium.net/
    pause
    exit /b 1
)

echo [1/3] Cleaning old build files...
if exist out rmdir /s /q out
mkdir out
echo       Done.

echo [2/3] Compiling Java source files...
javac -d out src\Message.java src\ChatAnalyzer.java src\Server.java src\ClientHandler.java src\ClientConnection.java src\MainFrame.java src\Main.java
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Compilation failed. Please check your Java source files.
    pause
    exit /b 1
)
echo       Compilation successful!

echo [3/3] Launching application...
echo.
java -cp out src.Main

pause
