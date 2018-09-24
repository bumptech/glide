#!/usr/bin/env bash

set -e

if [ ! "$firebase_enabled" == "true" ]; then
  echo "Unable to run Firebase tests for pull requests, exiting"
  exit 0
fi

./gradlew :instrumentation:assembleDebug \
  :instrumentation:assembleDebugAndroidTest \
  --parallel \
  -PDISABLE_ERROR_PRONE &
pid=$!
./scripts/install_firebase.sh
wait $pid

apk_dir=instrumentation/build/outputs/apk
./google-cloud-sdk/bin/gcloud firebase test android run \
  --type instrumentation \
  --app $apk_dir/debug/instrumentation-debug.apk \
  --test $apk_dir/androidTest/debug/instrumentation-debug-androidTest.apk \
  --device model=Nexus6P,version=27,locale=en,orientation=portrait \
  --device model=Nexus6P,version=26,locale=en,orientation=portrait \
  --device model=Nexus6P,version=25,locale=en,orientation=portrait \
  --device model=Nexus6P,version=24,locale=en,orientation=portrait \
  --device model=Nexus6P,version=23,locale=en,orientation=portrait \
  --device model=Nexus6,version=22,locale=en,orientation=portrait \
  --device model=Nexus5,version=21,locale=en,orientation=portrait \
  --device model=Nexus5,version=19,locale=en,orientation=portrait \
  --project android-glide \
  --no-auto-google-login \
