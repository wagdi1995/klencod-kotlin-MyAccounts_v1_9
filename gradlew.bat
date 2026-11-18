@rem Gradle startup script for Windows (compact version)
@if "%DEBUG%"=="" @echo off
setlocal
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
if defined JAVA_HOME (set JAVA_EXE=%JAVA_HOME%\bin\java.exe) else (set JAVA_EXE=java.exe)
"%JAVA_EXE%" -version >NUL 2>&1
if %ERRORLEVEL% neq 0 (
  echo ERROR: Java not found. Set JAVA_HOME or add java to PATH. 1>&2
  exit /b 1
)
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%CLASSPATH%" (
  echo ERROR: gradle\wrapper\gradle-wrapper.jar is missing. Open the project in Android Studio or run: gradle wrapper --gradle-version 9.3.1 1>&2
  exit /b 1
)
"%JAVA_EXE%" -Xmx64m -Xms64m -Dfile.encoding=UTF-8 -Dorg.gradle.appname=gradlew -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
