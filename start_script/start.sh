#!/bin/bash

APP_NAME="java-all-call-graph-server"
OUTPUT_ROOT_PATH="."

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

JAR_FILE=""
for f in ${APP_NAME}*.jar; do
    if [ -f "$f" ]; then
        case "$f" in
            *-sources.jar|*-javadoc.jar) continue ;;
        esac
        JAR_FILE="$f"
        break
    fi
done

if [ -z "$JAR_FILE" ]; then
    echo "Error: JAR file not found: ${APP_NAME}"
    echo "Please copy this shell script to the directory containing the jar file"
    read -p "Press Enter to continue..."
    exit 1
fi

echo "Using JAR file: ${JAR_FILE}"

JVM_OPTS="-Xms512m -Xmx2048m"

CONF_DIR="${SCRIPT_DIR}/conf"

if [ ! -d "$CONF_DIR" ]; then
    echo "Warning: Config directory not found: ${CONF_DIR}"
fi

# run
echo ""
echo "============================================================"
echo "Starting ${APP_NAME}"
echo "============================================================"
echo ""

java ${JVM_OPTS} -Djacgserver.output.root.path="${OUTPUT_ROOT_PATH}" -Dspring.config.additional-location="file:${CONF_DIR}/" -Dlog4j2.configurationFile="${CONF_DIR}/log4j2.xml" -jar "${JAR_FILE}"
