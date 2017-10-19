#!/usr/bin/env bash

set -e

if [ "$COMPONENT" == "instrumentation" ]; then
  echo "Starting emulator for $COMPONENT tests"
  ./scripts/travis_create_emulator.sh &

fi

