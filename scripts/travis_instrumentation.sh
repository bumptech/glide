#!/usr/bin/env bash

set -e

./gradlew :instrumentation:assembleDebug :instrumentation:assembleDebugAndroidTest --parallel

echo "Waiting for emulator..."
android-wait-for-emulator

./gradlew :instrumentation:connectedDebugAndroidTest

