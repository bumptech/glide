#!/usr/bin/env bash

set -e

# TODO(judds): Remove the KSP tests when support is available to run them in
# Google3
./gradlew build \
  -x :samples:flickr:build \
  -x :samples:giphy:build \
  -x :samples:contacturi:build \
  -x :samples:gallery:build \
  -x :samples:imgur:build \
  -x :samples:svg:build \
  :instrumentation:assembleAndroidTest \
  :benchmark:assembleAndroidTest \
  :glide:releaseJavadoc \
  :annotation:ksp:test:test \
  :integration:ktx:apiCheck \
  --parallel
