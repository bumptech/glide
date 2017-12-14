#!/usr/bin/env bash

set -e

if [ -z ${encrypted_ad2664a1c4dd_key+x} ] || [ -z ${encrypted_ad2664a1c4dd_iv+x} ] || [ -z ${GCLOUD_FILE} ]; then
  export firebase_enabled="false"
else
  export firebase_enabled="true"
fi


if [ "$COMPONENT" == "unit" ]; then
  ./scripts/travis_unit.sh
elif [ "$COMPONENT" == "instrumentation" ]; then
  ./scripts/travis_instrumentation.sh
elif [ "$COMPONENT" == "samples" ]; then
  ./scripts/travis_samples.sh
elif [ "$COMPONENT" == "firebase" ]; then
  ./scripts/travis_firebase.sh
else
  echo "Unrecognized component: $COMPONENT"
  exit 1
fi
