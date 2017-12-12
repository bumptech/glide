#!/usr/bin/env bash

set -e

if [ "$#" -ne 1 ]; then
  echo "Usage: ./release_checks.sh <major.minor.patch[-SNAPSHOT]>"
  exit 1
fi

if [[ $(git status -uno --porcelain) ]]; then
  echo "One or more changes, commit or revert first."
  git status -uno --porcelain
  exit 1
fi
if [[ $(git rev-list master...bump/master --count) -ne 0 ]]; then
  echo "Bump and master are not up to date"
  git rev-list master...bump/master --pretty
  exit 1
fi
if [[ $(git rev-list master...origin/master --count) -ne 0 ]]; then
  echo "Origin and master are not up to date"
  git rev-list master...origin/master --pretty
  exit 1
fi
if [[ $(git ls-files --exclude-standard --others) ]]; then
  echo "Untracked files, aborting"
  exit 1
fi

version=$1
echo "Setting version to $version"
echo -n "Is this a correct? (y/n)? "
read answer
if echo "$answer" | grep -iq "^y" ; then
  echo "Updating gradle.properties..."
else
  echo "Cancelling"
  exit 1
fi

sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=${version}/g" gradle.properties
sed -i '' "s/VERSION_MAJOR=.*/VERSION_MAJOR=$(echo $version | cut -d '.' -f 1)/" gradle.properties
sed -i '' "s/VERSION_MINOR=.*/VERSION_MINOR=$(echo $version | cut -d '.' -f 2)/" gradle.properties
sed -i '' "s/VERSION_PATCH=.*/VERSION_PATCH=$(echo $version | cut -d '.' -f 3 | sed 's/-.*//')/" gradle.properties

git diff

echo "Updated gradle.properties, is this correct? (y/n)?"
read answer
if echo "$answer" | grep -iq "^y" ; then
  echo "Committing..."
else
  echo "Cancelling"
  exit 1
fi

version_tag="v${version}"
git add gradle.properties
git commit -m "Bump version to ${version}"
if [[ $version != *"SNAPSHOT"* ]]; then
  echo "Found release version, adding tag, building and uploading"
  git tag $version_tag

  echo "Building... and uploading"
  ./gradlew clean build --parallel
  ./gradlew uploadArchives 

  echo "Upload complete, please verify the output and upload the jars to the GitHub release."
fi

echo -n "Ready to push, continue? (y/n)? "
read answer
if echo "$answer" | grep -iq "^y" ; then
  echo "Pushing commits"
else
  echo "Cancelling"
  exit 1
fi

git push origin master
git push bump master
if [[ $version != *"SNAPSHOT"* ]]; then
  echo "Found release version, pushing tags"
  git push origin $version_tag
  git push bump $version_tag
fi

