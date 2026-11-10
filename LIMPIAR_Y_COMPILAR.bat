@echo off
setlocal
cd /d "%~dp0"
echo ==========================================
echo   CLICKWORK - LIMPIEZA Y COMPILACION
echo ==========================================
echo.
echo Este proceso elimina SOLO archivos generados de Gradle.
echo No elimina codigo fuente ni datos de Supabase.
echo.
call gradlew.bat --stop
if exist ".gradle" rmdir /s /q ".gradle"
if exist "app\build" rmdir /s /q "app\build"
if exist "build" rmdir /s /q "build"
echo.
echo Iniciando compilacion Debug...
call gradlew.bat clean assembleDebug
if errorlevel 1 (
  echo.
  echo ERROR: Gradle no pudo completar la compilacion.
  echo Revisa la pestaña Build de Android Studio.
  pause
  exit /b 1
)
echo.
echo COMPILACION CORRECTA.
echo APK: app\build\outputs\apk\debug\app-debug.apk
pause
