#!/usr/bin/env bash
#
# Loops through all Android API levels that Glide supports (and that 
# functioning emulators exist for) and runs a particular emulator test file
# to generate canonical assets. If assets start to fail on a particular sdk
# level, the test file is updated with the new API level to split on and 
# assets for that particular api level are added to the test resources 
# directory.
#
# Usage:
#   ./scripts/split_by_sdk.sh [--abis x86,armeabi-v7a] [--apis 16,17] \
#      [-v/--verbose] [--tests com.bumptech.glide.TestName1,com.bumptech.glide.TestName2]
#
# apis: The Android SDK version(s) you want to run against.
# abis: The Android CPU types you want to run against
# v/verbose: Enable verbose logging.

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    --tests)
    test_classes_string="$2"
    shift # past argument
    shift # past value
    ;;
    --apis)
    apis_string="$2"
    shift # past argument
    shift # past value
    ;;
    --abis)
    abis_string="$2"
    shift # past argument
    shift # past value
    ;;
    -v|--verbose)
    verbose="1"
    shift # past argument
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters

if [ -z "$test_classes_string" ]; then
  test_classes_string=`grep -rwl instrumentation/src/androidTest -e RegressionTest \
    | grep -v "/test/" \
    | grep -v ".bak" \
    | tr '\n' ',' \
    | sed 's/instrumentation\/src\/androidTest\/java\///g' \
    | sed 's/\//\./g' \
    | sed 's/\.java//g' \
    | sed 's/,*$//g'` 
fi

if [ -z "$apis_string" ]; then
  declare -a apis=(
                  "16" 
                  #"17" API 17 emulator seems to have trouble starting and I haven't yet found a case where behaviors changed at that API level.
                  "18"
                  "19"
                  # "20" Android Wear, missing x86 emulators.
                  "21"
                  "22"
                  "23"
                  "24"
                  "25"
                  "26")
else 
  IFS=',' read -ra apis <<< "$apis_string"
fi

if [ -z "$abis_string" ]; then
  declare -a abis=(
  "x86"
  "armeabi-v7a"
  )
else 
  IFS=',' read -ra abis <<< "$abis_string"
fi

IFS=',' read -ra test_classes <<< "$test_classes_string"
for test_class in "${test_classes[@]}"
do
  test_path=`echo $test_class | sed 's/\./\//g' | sed -e 's/^/instrumentation\/src\/androidTest\/java\//' | sed -e 's/$/\.java/'`
  if [ ! -f "${test_path}" ]; then
    echo "Missing test $test_class at expected path: $test_path"
    exit 1
  fi
done

if (($verbose)); then
  printf "tests: "
  printf '%s,' "${test_classes[@]}"
  printf "\nabis:"
  printf '%s,' "${abis[@]}"
  printf "\napis:"
  printf '%s,' "${apis[@]}"
  printf "\n"
fi

adb devices | grep -v "List of devices" | grep device > /dev/null 2>&1 \
  && \
  { \
    echo "Emulators are already running, kill them before running this script: "; \
    echo "e.g.: adb -s emulator-5554 emu kill"; \
    adb devices; \
    exit 1; \
  }

exec 3>&1
exec 4>&2
if !(($verbose)); then
  exec 1>/dev/null
  exec 2>/dev/null
fi

for abi in "${abis[@]}"
do
  if [ "${abi}" == "armeabi-v7a" ]; then
    emulator_type="default"
    emulator_script=$ANDROID_HOME/emulator/emulator
  else 
    emulator_type="google_apis"
    emulator_script=$ANDROID_HOME/tools/emulator
  fi

  for api in "${apis[@]}"
  do
    if [ "${abi}" == "armeabi-v7a" ] && [ "${api}" -gt 22 ]; then
      echo "armeabi-v7a emulators beyond API 22 are unreliable, ignoring ${api}"
      continue
    fi

    echo "Checking on API ${api} and ${abi}" 1>&3 2>&4
    target="system-images;android-${api};${emulator_type};${abi}"
    sdkmanager --install $target
    avdmanager create avd --force -n test -k $target --device "Nexus 5X" -c 2000M 
    QEMU_AUDIO_DRV=none $emulator_script -avd test -no-window &
    pid=$!
    ./scripts/android-wait-for-emulator.sh
    for test_class in "${test_classes[@]}"
    do
      test_path=`echo $test_class | sed 's/\./\//g' | sed -e 's/^/instrumentation\/src\/androidTest\/java\//' | sed -e 's/$/\.java/'`
      ./gradlew :instrumentation:connectedCheck \
        -Pandroid.testInstrumentationRunnerArguments.class=$test_class
      if [ $? -ne 0 ]; then
        echo "Tests for API ${api} failed, updating SplitBySdk and generating resources..." 1>&3 2>&4
        if [ -z $(grep "@SplitBySdk" $test_path | grep "${api}") ]; then
          sed -i.bak s/@SplitBySdk\(\{/@SplitBySdk\(\{$api,/ $test_path
          rm "${test_path}.bak"
        fi
        ./scripts/regenerate_resources.sh $test_class #|| { echo "Tests still fail with new resources, aborting";  exit 1; }
      fi
    done
    adb -s emulator-5554 emu kill
    sleep 1
    kill -9 $pid
    pkill emulator64-crash-service
    pkill emulator-crash-service
    echo "Finished API ${api}" 1>&3 2>&4
  done
done
