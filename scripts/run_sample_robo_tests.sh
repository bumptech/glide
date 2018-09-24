#!/usr/bin/env bash
# Runs Firebases' robo tests (monkeyrunner) on Glide's sample apps
#
# Usage: 
# ./scripts/run_sample_robo_tests.sh

set -e

./gradlew :samples:flickr:build \
  :samples:giphy:build \
  :samples:contacturi:build \
  :samples:gallery:build \
  :samples:imgur:build \
  :samples:svg:build \
  --parallel

declare -a samples=("flickr" 
                "giphy" 
                "contacturi"
                "gallery"
                "imgur"
                "svg")
pids=()

for sample in "${samples[@]}"
do
  sample_dir="samples/${sample}/build/outputs/apk/"
  sample_apk="${sample_dir}/${sample}-debug.apk"
  gcloud firebase test android run \
    --type robo \
    --app $sample_apk \
    --device model=Nexus6P,version=26,locale=en,orientation=portrait  \
    --project android-glide \
    --no-auto-google-login &
  pids+=("$!")
done

for current in "${pids[@]}"
do
  wait $current
done
       


