#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 1 || ! -f "$1" ]]; then
    printf 'Usage: bash scripts/smoke-test-zip.sh path/to/runnable.zip\n' >&2
    exit 2
fi

scripts_dir="$(cd "$(dirname "$0")" && pwd)"
zip="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}"
stage="$(mktemp -d "${TMPDIR:-/tmp}/mdi-optics-smoke.XXXXXX")"
trap 'rm -rf -- "$stage"' EXIT

unzip -tq "$zip"
unzip -q "$zip" -d "$stage/release"
jars=("$stage"/release/mdi-optics-*/mdi-optics.jar)
if [[ ${#jars[@]} != 1 || ! -f "${jars[0]}" || ! -d "$(dirname "${jars[0]}")/lib" ]]; then
    printf 'Expected one extracted mdi-optics-*/mdi-optics.jar with a bundled lib directory.\n' >&2
    exit 1
fi
launcher="${jars[0]}"

mkdir "$stage/classes" "$stage/home"
"${java_bin}javac" --release 17 -cp "$launcher" -d "$stage/classes" "$scripts_dir/ReleaseDesktopSmoke.java"
printf 'Premain-Class: ReleaseDesktopSmoke\n' > "$stage/agent.mf"
"${java_bin}jar" cfm "$stage/smoke-agent.jar" "$stage/agent.mf" -C "$stage/classes" .

cd "$(dirname "$launcher")"
# The agent observes and exercises the app started by the release JAR's own manifest.
"${java_bin}java" -Duser.home="$stage/home" -Djava.util.prefs.userRoot="$stage/home" -javaagent:"$stage/smoke-agent.jar" -jar "$launcher"
