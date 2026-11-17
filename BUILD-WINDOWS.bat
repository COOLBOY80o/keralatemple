@echo off
REM Builds the mod jar. Needs JDK 17 and an internet connection (first build downloads Forge, about 5-10 minutes).
java -version 2>&1 | findstr /C:"17." >nul
if errorlevel 1 (
  echo.
  echo This needs Java 17. Install "Temurin JDK 17" from https://adoptium.net then run this again.
  echo If Java 17 is installed but another version is first on your PATH, set JAVA_HOME to the JDK 17 folder.
  pause
  exit /b 1
)
call gradlew.bat build
if errorlevel 1 (
  echo.
  echo The build failed. Copy the red error text above and send it to Claude to fix.
  pause
  exit /b 1
)
echo.
echo Done. Your mod is in the build\libs folder: keralatemple-1.0.0.jar
echo Copy it into the "mods" folder of a Minecraft 1.20.1 Forge installation.
start "" build\libs
pause
