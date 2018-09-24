#!/usr/bin/env bash
# Copies our debug.keystore file to its expected location to avoid a bug
# where the Android build system seems to occasionally fail to generate it.

set -e

cp debug.keystore ~/.android/debug.keystore
