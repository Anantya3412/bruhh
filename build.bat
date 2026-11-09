@echo off
title Build RivalSMP
echo Building RivalSMP...
where java >nul 2>nul
if errorlevel 1 (
  echo Java was not found. Install JDK 21 first.
  pause
  exit /b 1
)
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found. Use GitHub Actions to build instead.
  pause
  exit /b 1
)
call mvn --batch-mode clean package
if errorlevel 1 (
  echo Build failed. Check the errors above.
  pause
  exit /b 1
)
echo.
echo Build successful. JAR is in the target folder.
pause
