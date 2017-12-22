#!/usr/bin/env bash

set -e

#installing xvfb needs accept android license
echo y | sdkmanager --update

./gradlew build \
  -x :samples:flickr:build \
  -x :samples:giphy:build \
  -x :samples:contacturi:build \
  -x :samples:gallery:build \
  -x :samples:imgur:build \
  -x :samples:svg:build \
  -x testReleaseUnitTest --parallel
