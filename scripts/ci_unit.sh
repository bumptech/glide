#!/usr/bin/env bash

set -e

./gradlew build \
  -x :library:test:testDebugUnitTest \
  :library:test:assembleDebugUnitTest \
  -x :library:testDebugUnitTest \
  :library:assembleDebugUnitTest \
  -x :annotation:ksp:test:testDebugUnitTest \
  :annotation:ksp:test:assembleDebugUnitTest \
  -x :third_party:disklrucache:testDebugUnitTest \
  :third_party:disklrucache:assembleDebugUnitTest \
  -x :integration:cronet:testDebugUnitTest \
  :integration:cronet:assembleDebugUnitTest \
  -x :integration:gifencoder:testDebugUnitTest \
  :integration:gifencoder:assembleDebugUnitTest \
  -x :integration:ktx:testDebugUnitTest \
  :integration:ktx:assembleDebugUnitTest \
  -x :integration:concurrent:testDebugUnitTest \
  :integration:concurrent:assembleDebugUnitTest \
  -x :integration:volley:testDebugUnitTest \
  :integration:volley:assembleDebugUnitTest \
  -x :integration:sqljournaldiskcache:testDebugUnitTest \
  :integration:sqljournaldiskcache:assembleDebugUnitTest \
  -x :third_party:gif_decoder:testDebugUnitTest \
  :third_party:gif_decoder:assembleDebugUnitTest \
  :samples:flickr:build \
  :samples:giphy:build \
  :samples:contacturi:build \
  :samples:gallery:build \
  :samples:imgur:build \
  :samples:svg:build \
  :instrumentation:assembleAndroidTest \
  :benchmark:assembleAndroidTest \
  :glide:releaseJavadoc \
  :annotation:ksp:test:test \
  :integration:ktx:apiCheck \
  :annotation:ksp:integrationtest:test \
  --parallel
