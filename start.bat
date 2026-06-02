@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul 2>&1
title InsightWrite 2.0

set "ENV_FILE=%~dp0.env"
set "BACKEND_DIR=%~dp0backend"
set "FRONTEND_DIR=%~dp0frontend"

set "MAVEN_CMD="
if exist "%BACKEND_DIR%\mvnw.cmd" (
  set "MAVEN_CMD=%BACKEND_DIR%\mvnw.cmd"
) else (
  where mvn.cmd >nul 2>&1
  if not errorlevel 1 set "MAVEN_CMD=mvn.cmd"
)
if not defined MAVEN_CMD (
  where mvn >nul 2>&1
  if not errorlevel 1 set "MAVEN_CMD=mvn"
)

if not defined MAVEN_CMD (
  echo.
  echo   Maven was not found.
  echo   Install Maven 3.9+ and make sure mvn or mvn.cmd is available in PATH,
  echo   or add a Maven Wrapper to backend\mvnw.cmd.
  echo.
  pause
  exit /b 1
)

where npm.cmd >nul 2>&1
if errorlevel 1 (
  where npm >nul 2>&1
  if errorlevel 1 (
    echo.
    echo   npm was not found.
    echo   Install Node.js 18+ and make sure npm is available in PATH.
    echo.
    pause
    exit /b 1
  )
)

if not exist "%FRONTEND_DIR%\node_modules" (
  echo.
  echo   Frontend dependencies are not installed.
  echo   Run: cd frontend && npm.cmd install
  echo.
  pause
  exit /b 1
)

if not exist "%ENV_FILE%" (
  echo.
  echo   Missing local environment file: %ENV_FILE%
  echo   Copy .env.example to .env, fill in your local database, JWT, API, and mail values, then run start.bat again.
  echo.
  pause
  exit /b 1
)

for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
  if not "%%A"=="" set "%%A=%%B"
)

set "MISSING_CONFIG="
for %%V in (DB_USERNAME DB_PASSWORD JWT_SECRET DEEPSEEK_API_KEY MAIL_USERNAME MAIL_PASSWORD) do (
  if not defined %%V set "MISSING_CONFIG=!MISSING_CONFIG! %%V"
)

if defined MISSING_CONFIG (
  echo.
  echo   Missing required values in .env:!MISSING_CONFIG!
  echo   Fill them in and run start.bat again.
  echo.
  pause
  exit /b 1
)

echo.
echo   InsightWrite 2.0
echo   ================
echo.
echo   Backend  http://localhost:8080
echo   Frontend http://localhost:5173
echo.
echo   Close this window (X) to stop both servers.
echo   -------------------------------------------
echo.

start /B "" cmd /c "cd /d %BACKEND_DIR% && %MAVEN_CMD% spring-boot:run"
start /B "" cmd /c "cd /d %FRONTEND_DIR% && npm.cmd run dev"

pause
