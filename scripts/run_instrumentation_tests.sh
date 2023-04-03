#!/usr/bin/env bash
# Runs instrumentation tests on firebase. Must be run locally, not on travis.
#
# Usage: 
# ./scripts/run_instrumentation_test.sh

./gradlew :instrumentation:assembleDebug :instrumentation:assembleDebugAndroidTest --parallel

apk_dir=instrumentation/build/outputs/apk
gcloud firebase test android run \
  --type instrumentation \
  --app $apk_dir/instrumentation-debug.apk \
  --test $apk_dir/instrumentation-debug-androidTest.apk \
  --device model=Nexus6P,version=26,locale=en,orientation=portrait  \
  --device model=Nexus6P,version=25,locale=en,orientation=portrait \
  --device model=Nexus6P,version=23,locale=en,orientation=portrait \
  --device model=Nexus6,version=22,locale=en,orientation=portrait \
  --device model=Nexus5,version=19,locale=en,orientation=portrait \
  --project android-glide \
  --no-auto-google-login \
