#!/usr/bin/env bash

set -e

echo no | android create avd --force -n test -t android-22 --abi armeabi-v7a
QEMU_AUDIO_DRV=none emulator -engine classic -avd test -no-window &

exit 0
