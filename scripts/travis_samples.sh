#!/usr/bin/env bash

set -e

#installing xvfb needs accept android license
echo y | sdkmanager --update

./gradlew :samples:flickr:build \
  :samples:giphy:build \
  :samples:contacturi:build \
  :samples:gallery:build \
  :samples:imgur:build \
  :samples:svg:build \
  --parallel &
pid=$!

if [ ! "$firebase_enabled" == "true" ]; then
  wait $pid
  echo "Unable to run Firebase tests for pull requests, exiting"
  exit 0
else
  ./scripts/install_firebase.sh
  wait $pid
fi


declare -a samples=("flickr"
                "giphy"
                "contacturi"
                "gallery"
                "imgur"
                "svg")
pids=()

for sample in "${samples[@]}"
do
  sample_dir="samples/${sample}/build/outputs/apk/debug"
  sample_apk="${sample_dir}/${sample}-debug.apk"
  ./google-cloud-sdk/bin/gcloud firebase test android run \
    --type robo \
    --app $sample_apk \
    --device model=Nexus6P,version=26,locale=en,orientation=portrait  \
    --project android-glide \
    --no-auto-google-login \
    --timeout 5m \
    &
  pids+=("$!")
done

for current in "${pids[@]}"
do
  wait $current
done


