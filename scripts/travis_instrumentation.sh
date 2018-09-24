#!/usr/bin/env bash

set -e

echo "Starting emulator for $COMPONENT tests"
./scripts/travis_create_emulator.sh &

./gradlew :instrumentation:assembleDebug \
  :instrumentation:assembleDebugAndroidTest \
  --parallel \
  -PDISABLE_ERROR_PRONE

echo "Waiting for emulator..."
android-wait-for-emulator

for i in {1..3}; do ./gradlew :instrumentation:connectedDebugAndroidTest && break; done

