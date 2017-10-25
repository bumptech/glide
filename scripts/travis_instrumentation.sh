#!/usr/bin/env bash

set -e

if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ $ANDROID_TARGET -gt 18 ]; then
  echo "Emulator tests >= 19 are run on Firebase ignoring"
  exit 0
fi

echo "Starting emulator for $COMPONENT tests"
./scripts/travis_create_emulator.sh &

./gradlew :instrumentation:assembleDebug :instrumentation:assembleDebugAndroidTest --parallel

echo "Waiting for emulator..."
android-wait-for-emulator

./gradlew :instrumentation:connectedDebugAndroidTest

