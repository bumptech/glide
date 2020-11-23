#!/usr/bin/env bash

set -e

./gradlew build \
  -x :samples:flickr:build \
  -x :samples:giphy:build \
  -x :samples:contacturi:build \
  -x :samples:gallery:build \
  -x :samples:imgur:build \
  -x :samples:svg:build \
  --parallel
./gradlew :instrumentation:assembleAndroidTest
