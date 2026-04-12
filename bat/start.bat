@echo off
setlocal enabledelayedexpansion

set APP_NAME=java-all-call-graph-server

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
    echo Please run this script in the directory containing the jar file
    pause
    exit /b 1
)

echo Found JAR file: %JAR_FILE%

set JVM_OPTS=-Xms512m -Xmx2048m
set OUTPUT_ROOT_PATH=.

:parse_args
if "%~1"=="" goto :run
if /i "%~1"=="-o" (
    set OUTPUT_ROOT_PATH=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--output" (
    set OUTPUT_ROOT_PATH=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="-h" goto :show_help
if /i "%~1"=="--help" goto :show_help
shift
goto :parse_args

:show_help
echo.
echo Usage: %~nx0 [options]
echo.
echo Options:
echo   -o, --output PATH   Set output root directory
echo   -h, --help          Show this help
echo.
echo Examples:
echo   %~nx0
echo   %~nx0 -o D:\output
echo   %~nx0 --output /data/output
echo.
pause
exit /b 0

:run
echo.
echo ============================================================
echo Starting %APP_NAME%
echo Output root: %OUTPUT_ROOT_PATH%
echo ============================================================
echo.

set CONF_DIR=%SCRIPT_DIR%conf

if not exist "%CONF_DIR%" (
    echo Warning: Config directory not found: %CONF_DIR%
)

java %JVM_OPTS% -Djacgserver.output.root.path="%OUTPUT_ROOT_PATH%" -Dspring.config.additional-location="file:%CONF_DIR%/" -Dlog4j2.configurationFile="file:%CONF_DIR%/log4j2.xml" -jar "%JAR_FILE%"

pause
