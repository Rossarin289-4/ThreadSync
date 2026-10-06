@echo off
REM Windows runner: compile เป็น Java 17 bytecode แล้วเปิด ThreadSync
cd /d "%~dp0.."
if not exist out mkdir out
javac --release 17 -d out src\threadsync\Main.java
if errorlevel 1 exit /b %errorlevel%
start "" cmd /c "timeout /t 2 >nul & start http://localhost:8080"
java -cp out threadsync.Main
