@echo off
setlocal
set "JAVA_HOME=%~dp0.jdk\jdk-17.0.18+8"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"
call gradlew.bat run
endlocal
