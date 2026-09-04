#!/bin/bash
# HadiAI build — final tuned three-phase (cgroup wall ~954M, hermes ~300M resident).
# Phase :core uses proven 200/200; phases :app use 255/205 (fits the real budget).
export JAVA_HOME=/opt/jdk17
export ANDROID_HOME=/opt/android-sdk
export MALLOC_ARENA_MAX=1
export GRADLE_OPTS="-Xmx24m -Xms16m -XX:MaxMetaspaceSize=48m -XX:+UseSerialGC -XX:-UsePerfData"
cd /data/workspace || { echo "FAIL: no project"; exit 1; }
GRADLE=$(echo /root/.gradle/wrapper/dists/gradle-8.9-bin/*/gradle-8.9/bin/gradle)
COMMON="-Xms24m -XX:MaxDirectMemorySize=10m -XX:ReservedCodeCacheSize=8m -XX:+UseSerialGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=15 -XX:ActiveProcessorCount=1 -XX:-UsePerfData -Xss256k -Dfile.encoding=UTF-8"
CORE="-Dorg.gradle.jvmargs=-Xmx200m -XX:MaxMetaspaceSize=200m $COMMON"
echo "=== HadiAI build start $(date) ==="
"$GRADLE" --no-daemon "-Dorg.gradle.jvmargs=-Xmx200m -XX:MaxMetaspaceSize=200m $COMMON" :core:compileDebugKotlin > /tmp/p-core.log 2>&1
c1=$?
echo "phase1 :core exit=$c1"
if [ "$c1" = "0" ]; then
  "$GRADLE" --no-daemon "-Dorg.gradle.jvmargs=-Xmx255m -XX:MaxMetaspaceSize=205m $COMMON" :app:compileDebugKotlin > /tmp/p-appc.log 2>&1
  c2=$?
  echo "phase2 :app:compileDebugKotlin exit=$c2"
  if [ "$c2" = "0" ]; then
    "$GRADLE" --no-daemon "-Dorg.gradle.jvmargs=-Xmx255m -XX:MaxMetaspaceSize=205m $COMMON" :app:assembleDebug > /tmp/p-assemble.log 2>&1
    c3=$?
    echo "phase3 :app:assembleDebug exit=$c3"
    APK=/data/workspace/app/build/outputs/apk/debug/app-debug.apk
    [ "$c3" = "0" ] && [ -f "$APK" ] && echo "APK_OK $APK $(stat -c%s "$APK")" || echo "APK_MISSING c3=$c3"
  else
    tr '\r' '\n' < /tmp/p-appc.log | grep -E "^e: |FAILED|Metaspace|What went wrong" | head -8
  fi
else
  tr '\r' '\n' < /tmp/p-core.log | grep -E "^e: |FAILED|Metaspace|What went wrong" | head -8
fi
echo "=== done oom=$(grep oom_kill /sys/fs/cgroup/memory.events | awk '{print $2}') ==="
