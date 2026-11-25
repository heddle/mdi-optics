#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
mvn --batch-mode clean verify org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath \
    -DincludeScope=runtime -Dmdep.outputFile=target/runtime-classpath.txt
python3 scripts/package-runnable.py "${1:-}"
