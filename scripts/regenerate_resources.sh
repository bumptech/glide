#!/usr/bin/env bash
#
# Generates or regenerates canonical resources for Glide's emulator tests with the cooperation
# of the BitmapRegressionTester class.
#
# Usage:
# ./scripts/regenerate_resources.sh <com.bumptech.glide.instrumentation.class_name>
#
# The class name is optional. If not specified all tests will be run (including those that
# do not generate resources).

# The signal file that tells BitmapRegressionTester to generate the resource files.
REGENERATE_FILE_NAME="regenerate"
# The name of the subfolder on the sdcard where resources are stored on the device/emulator.
DIRECTORY_NAME="test_files"
# The full path to a place where the app is able to write resources and we're able to read them.
DIRECTORY="/sdcard/DCIM/${DIRECTORY_NAME}"

set -e

if [ "$#" -eq 1 ]; then
  test_restriction="-Pandroid.testInstrumentationRunnerArguments.class=${1}"
fi

exec 3>&1
exec 4>&2
if !(($VERBOSE)); then
  exec 1>/dev/null
  exec 2>/dev/null
fi

echo "Setting up environment..."  1>&3 2>&4
adb devices | grep -v "List of devices" | grep device \
  || echo "No devices found, try starting an emulator" 1>&3 2>&4

adb root || true 
# In case there are any old artifacts from an old or failed test, clean them up.
adb shell rm -r $DIRECTORY || true
# Create the signal file.
# On some emulators touch fails if the directory isn't created first.
adb shell mkdir /sdcard/DCIM || true
adb shell mkdir $DIRECTORY || true
# This actually has to work, previous steps may fail if the directories already exist.
adb shell touch "${DIRECTORY}/${REGENERATE_FILE_NAME}"

# On APIs > 22 we need to grant the appropriate runtime permissions so our test APK can write
# resource files to the public sdcard. Cache and internal cache directories aren't consistently
# available across all versions of Android. So far this is the best cross SDK solution I've 
# found
sdk_version=`adb shell getprop ro.build.version.sdk`
sdk_version=`echo $sdk_version | tr -d '\r'`
if [[ $sdk_version -gt 22 ]]; then
  echo "Installing apks and granting runtime permissions..." 1>&3 2>&4
  ./gradlew :instrumentation:installDebug :instrumentation:installDebugAndroidTest 
  adb shell pm grant com.bumptech.glide.instrumentation android.permission.WRITE_EXTERNAL_STORAGE
  adb shell pm grant com.bumptech.glide.instrumentation android.permission.READ_EXTERNAL_STORAGE
  adb shell pm grant com.bumptech.glide.instrumentation.test android.permission.WRITE_EXTERNAL_STORAGE
  adb shell pm grant com.bumptech.glide.instrumentation.test android.permission.READ_EXTERNAL_STORAGE
fi

echo "Generating updated resource files..." 1>&3 2>&4
./gradlew :instrumentation:connectedDebugAndroidTest $test_restriction --parallel || true 

echo "Copying updated resource files to res directory..." 1>&3 2>&4
adb pull $DIRECTORY
rm "${DIRECTORY_NAME}/${REGENERATE_FILE_NAME}" 
cp $DIRECTORY_NAME/raw/* instrumentation/src/main/res/raw 
rm -rf $DIRECTORY_NAME
adb shell rm -r $DIRECTORY
 
echo "Verifying all tests pass..." 1>&3 2>&4

./gradlew :instrumentation:clean
./gradlew :instrumentation:connectedDebugAndroidTest $test_restriction --parallel

echo "Complete!" 1>&3 2>&4
