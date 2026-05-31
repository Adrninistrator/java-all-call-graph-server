@echo off
setlocal enabledelayedexpansion

set APP_NAME=java-all-call-graph-server
set OUTPUT_ROOT_PATH=.

set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

set JAR_FILE=
for %%f in ("%APP_NAME%*.jar") do (
    if not "%%f"=="%APP_NAME%-sources.jar" (
        if not "%%f"=="%APP_NAME%-javadoc.jar" (
            set JAR_FILE=%%f
        )
    )
)

if "%JAR_FILE%"=="" (
    echo Error: JAR file not found: %APP_NAME%
    echo Please copy this bat script to the directory containing the jar file
    pause
    exit /b 1
)

echo Using JAR file: %JAR_FILE%

set JVM_OPTS=-Xms512m -Xmx2048m

set CONF_DIR=%SCRIPT_DIR%conf

if not exist "%CONF_DIR%" (
    echo Warning: Config directory not found: %CONF_DIR%
)

:run
echo.
echo ============================================================
echo Starting %APP_NAME%
echo ============================================================
echo.

rem relace / to \
set CONF_DIR_URI=%CONF_DIR:\=/%

java %JVM_OPTS% -Djacgserver.output.root.path="%OUTPUT_ROOT_PATH%" -Dspring.config.additional-location="file:%CONF_DIR_URI%/" -Dlog4j2.configurationFile="%CONF_DIR%\log4j2.xml" -jar "%JAR_FILE%"

pause
