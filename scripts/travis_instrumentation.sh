#!/usr/bin/env bash

set -e

./gradlew :library:assembleDebug :library:assembleDebugAndroidTest --parallel

echo "Waiting for emulator..."
android-wait-for-emulator

./gradlew :instrumentation:connectedDebugAndroidTest

