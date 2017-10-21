#!/usr/bin/env bash

set -e

echo y | android --silent update sdk --no-ui --all --filter android-$ANDROID_TARGET
echo y | android --silent update sdk --no-ui --all --filter sys-img-armeabi-v7a-android-$ANDROID_TARGET
echo no | android create avd --force -n test -t android-$ANDROID_TARGET --abi armeabi-v7a
QEMU_AUDIO_DRV=none emulator -engine classic -avd test -no-window &

exit 0
