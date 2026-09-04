#!/bin/bash
# AvaChat :app compile — agent-free wrapper (run OUTSIDE the agent to free ~45M RSS)
# Usage: nohup bash build_app.sh > /opt/setup/agentless.log 2>&1 &
set -u
export JAVA_HOME=/opt/jdk
export MALLOC_ARENA_MAX=1
export GRADLE_OPTS="-Xmx32m -Xms16m -XX:+UseSerialGC -XX:-UsePerfData"
cd /data/workspace/AvaChat || exit 1
LOG=/opt/setup/build_app_agentless.log
echo "=== agentless :app:compileDebugKotlin start $(date) ===" > "$LOG"
/opt/gradle-8.9/bin/gradle --no-daemon :app:compileDebugKotlin >> "$LOG" 2>&1
code=$?
echo "EXIT:$code" >> "$LOG"
echo "=== done $(date), exit=$code ==="
if [ "$code" = "0" ]; then
  /opt/gradle-8.9/bin/gradle --no-daemon :app:assembleDebug >> "$LOG" 2>&1
  echo "ASSEMBLE_EXIT:$?" >> "$LOG"
fi
