#!/bin/sh
# This script installs the necessary Android dependencies to compile Glide and run
# the test suite.
# 
# Pre-requisites: 
# Using the android sdk tool, under Android 4.4.2 (API 19), install:
#   SDK Platform
#   Glass Development Kit Preview
# Also using the android sdk tool, under Android 4.4.0 (API 14), install:
#   SDK Platform

git clone https://github.com/mosabua/maven-android-sdk-deployer.git
cd maven-android-sdk-deployer 
mvn clean install -N && cd platforms && mvn clean install -N && cd android-19 && mvn clean install || {
  echo 'Failed to install 4.4 SDK, install relevant packages in android SDK first';
  exit 1; 
}
cd ../..
mvn clean install -N && cd platforms && mvn clean install -N && cd android-14 && mvn clean install || {
  echo 'Failed to install 4.0 SDK, install relevant packages in android SDK first';
  exit 1;
}
cd ../..
mvn clean install -N && cd extras && mvn clean install -N && cd compatibility-v4 && mvn clean install || {
  echo 'Failed to install android-support-v4, install support library in android SDK first';
  exit 1;
}
cd ../../..
rm -rf maven-android-sdk-deployer
