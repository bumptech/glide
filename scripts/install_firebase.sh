#!/usr/bin/env bash

set -e

openssl aes-256-cbc -K $encrypted_ad2664a1c4dd_key -iv $encrypted_ad2664a1c4dd_iv -in $GCLOUD_FILE -out gcloud.json -d

wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-176.0.0-linux-x86_64.tar.gz
tar xf google-cloud-sdk-176.0.0-linux-x86_64.tar.gz
echo "y" | ./google-cloud-sdk/bin/gcloud components update beta
./google-cloud-sdk/bin/gcloud auth activate-service-account --key-file gcloud.json
