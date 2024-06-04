#!/bin/sh
if [ -z "$1" ] || [ -z "$2" ]; then
    echo "ERROR: Two arguments expected: 1) configuration file and 2) tools file"
    exit 1
fi

java -jar /app/server.jar --agent.config=$1 --agent.tools=$2