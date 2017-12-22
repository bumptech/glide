#!/usr/bin/env bash

set -e

target="system-images;android-${ANDROID_TARGET};default;armeabi-v7a"
echo y | sdkmanager --update
echo y | sdkmanager --install $target
avdmanager create avd --force -n test -k $target --device "Nexus 5X" -c 2000M

if [ "$ANDROID_TARGET" == "16" ] || [ "$ANDROID_TARGET" == "17" ] || [ "$ANDROID_TARGET" == "18" ]; then
  export DISPLAY=:99.0
  /usr/bin/Xvfb :99 -screen 0 1280x1024x24 &
  sleep 3 # give xvfb some time to start
  QEMU_AUDIO_DRV=none $ANDROID_HOME/emulator/emulator -avd test -memory 2048 &
else
  QEMU_AUDIO_DRV=none $ANDROID_HOME/emulator/emulator -avd test -no-window -memory 2048 &
fi

exit 0
