#!/usr/bin/env bash

set -e

./gradlew build \
  -x :library:test:testDebugUnitTest \
  :library:test:assembleDebugUnitTest \
  -x :library:testDebugUnitTest \
  :library:assembleDebugUnitTest \
  -x :annotation:ksp:test:testDebugUnitTest \
  :annotation:ksp:test:assembleDebugUnitTest \
  -x :disklrucache:testDebugUnitTest \
  :disklrucache:assembleDebugUnitTest \
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
  -x :gif_decoder:testDebugUnitTest \
  :gif_decoder:assembleDebugUnitTest \
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
