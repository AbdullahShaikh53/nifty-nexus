@echo off
REM Lightweight Maven wrapper for Windows: uses mvn if installed, otherwise downloads Maven 3.9.9 once.
setlocal
where java >nul 2>nul
if errorlevel 1 (
  echo JDK 17+ is required but "java" was not found. Install from https://adoptium.net and restart VS Code.
  exit /b 1
)
where mvn >nul 2>nul
if errorlevel 1 goto wrapper
call mvn %*
exit /b

:wrapper
set "VER=3.9.9"
set "WDIR=%USERPROFILE%\.m2\wrapper"
set "MDIR=%WDIR%\apache-maven-%VER%"
if exist "%MDIR%\bin\mvn.cmd" goto run
echo Downloading Apache Maven %VER% (one time only)...
if not exist "%WDIR%" mkdir "%WDIR%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%VER%/apache-maven-%VER%-bin.zip' -OutFile '%WDIR%\maven.zip'; Expand-Archive -Force '%WDIR%\maven.zip' '%WDIR%'; Remove-Item '%WDIR%\maven.zip'"
if not exist "%MDIR%\bin\mvn.cmd" (
  echo Maven download failed. Check your internet connection, or install Maven from https://maven.apache.org
  exit /b 1
)
:run
call "%MDIR%\bin\mvn.cmd" %*
exit /b
