#!/usr/bin/env bash

set -e

if [ "$COMPONENT" == "unit" ]; then
  ./scripts/travis_sonatype_publish.sh
  ./gradlew jacocoTestReport coveralls
fi
