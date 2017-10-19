#!/usr/bin/env bash

set -e

if [ "$COMPONENT" == "unit" ]; then
  ./scripts/travis_unit.sh
elif [ "$COMPONENT" == "instrumentation" ]; then
  ./scripts/travis_instrumentation.sh
elif [ "$COMPONENT" == "samples" ]; then
  ./scripts/travis_samples.sh
else
  echo "Unrecognized component: $COMPONENT"
  exit 1
fi
