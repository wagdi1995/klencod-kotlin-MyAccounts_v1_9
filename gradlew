#!/bin/sh
# Gradle start up script for POSIX (compact version)
app_path=$0
while [ -h "$app_path" ]; do
  ls=$(ls -ld -- "$app_path"); link=${ls#*' -> '}
  case $link in /*) app_path=$link ;; *) app_path=${app_path%/*}/$link ;; esac
done
APP_HOME=$(cd "${app_path%/*}" >/dev/null && pwd -P) || exit
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ -n "$JAVA_HOME" ]; then JAVACMD=$JAVA_HOME/bin/java; else JAVACMD=java; fi
if ! command -v "$JAVACMD" >/dev/null 2>&1; then
  echo "ERROR: Java not found. Set JAVA_HOME or add java to PATH." >&2; exit 1
fi
if [ ! -f "$CLASSPATH" ]; then
  echo "ERROR: gradle/wrapper/gradle-wrapper.jar is missing. Open the project in Android Studio or run: gradle wrapper --gradle-version 9.3.1" >&2; exit 1
fi
exec "$JAVACMD" -Xmx64m -Xms64m -Dfile.encoding=UTF-8 \
  -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
