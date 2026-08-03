@ECHO OFF
SETLOCAL
SET WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar
SET PROJECT_DIR=%~dp0
SET PROJECT_DIR=%PROJECT_DIR:~0,-1%
IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Maven wrapper JAR is missing: %WRAPPER_JAR%
  EXIT /B 1
)
java "-Dmaven.multiModuleProjectDirectory=%PROJECT_DIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
ENDLOCAL
