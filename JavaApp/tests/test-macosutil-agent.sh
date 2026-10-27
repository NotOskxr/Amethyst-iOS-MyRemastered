#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
JDK_BIN="${BOOTJDK:-${JAVA_HOME:+$JAVA_HOME/bin}}"
JDK_BIN="${JDK_BIN:-$(dirname "$(command -v javac)")}"
BUILD=$(mktemp -d)
trap 'rm -rf "$BUILD"' EXIT
mkdir -p "$BUILD/patchjna_agent" "$BUILD/game" "$BUILD/tests" "$BUILD/old"
"$JDK_BIN/javac" -source 8 -target 8 -d "$BUILD/patchjna_agent" $(find src/patchjna_agent -name '*.java')
# Exercise the production packaging rule, without compiling unrelated modules.
make "$BUILD/patchjna_agent.jar" OUTPUTDIR="$BUILD" CLASSES= BOOTJDK="$JDK_BIN"
"$JDK_BIN/javac" -source 8 -target 8 -d "$BUILD/game" \
    tests/macosutil/com/mojang/blaze3d/platform/MacosUtil.java \
    tests/macosutil/com/sun/jna/Platform.java
"$JDK_BIN/javac" -source 8 -target 8 -d "$BUILD/old" tests/macosutil/older/MacosUtil.java
OLD_MAC="$BUILD/old/com/mojang/blaze3d/platform/MacosUtil.class"
"$JDK_BIN/javac" -source 8 -target 8 -cp "$BUILD/patchjna_agent.jar" \
    -d "$BUILD/tests" tests/macosutil/AgentRegression.java
# Without the agent this must fail at the simulated desktop native load.
if "$JDK_BIN/java" -cp "$BUILD/tests:$BUILD/patchjna_agent.jar" \
    AgentRegression "$BUILD/patchjna_agent.jar" "$BUILD/game" "$OLD_MAC" "$BUILD/game" > "$BUILD/control.log" 2>&1; then
    echo 'FAIL: regression fixture passed without the agent'; exit 1
fi
grep -q amethyst_test_desktop_jna_must_not_load "$BUILD/control.log"
"$JDK_BIN/java" -Xverify:all -javaagent:"$BUILD/patchjna_agent.jar" \
    -cp "$BUILD/tests:$BUILD/patchjna_agent.jar" \
    AgentRegression "$BUILD/patchjna_agent.jar" "$BUILD/game" "$OLD_MAC" "$BUILD/game"
# Also check on the selected runtime (e.g. JAVA_RUNTIME=java for Java 21/25).
if [ -n "${JAVA_RUNTIME:-}" ]; then
    "$JAVA_RUNTIME" -Xverify:all -javaagent:"$BUILD/patchjna_agent.jar" \
        -cp "$BUILD/tests:$BUILD/patchjna_agent.jar" \
        AgentRegression "$BUILD/patchjna_agent.jar" "${MC_CLIENT_JAR:-$BUILD/game}" "$OLD_MAC" "$BUILD/game"
fi
