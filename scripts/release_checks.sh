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
  echo "Updating gradle.properties"
else
  echo "Cancelling"
  exit 1
fi

sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=${version}/g" gradle.properties
sed -i '' "s/VERSION_MAJOR=.*/VERSION_MAJOR=$(echo $version | cut -d '.' -f 1)/" gradle.properties
sed -i '' "s/VERSION_MINOR=.*/VERSION_MINOR=$(echo $version | cut -d '.' -f 2)/" gradle.properties
sed -i '' "s/VERSION_PATCH=.*/VERSION_PATCH=$(echo $version | cut -d '.' -f 3 | sed 's/-.*//')/" gradle.properties

if [[ $version != *"SNAPSHOT"* ]]; then
  echo "Found release version, update README"
  sed -i '' "s:<version>.*</version>:<version>${version}</version>:" README.md
  sed -i '' "s/'com.github.bumptech.glide:glide:.*'/'com.github.bumptech.glide:glide:${version}'/" README.md
  sed -i '' "s/'com.github.bumptech.glide:compiler:.*'/'com.github.bumptech.glide:compiler:${version}'/" README.md
fi

git diff

echo "Updated files, is this correct? (y/n)?"
read answer
if echo "$answer" | grep -iq "^y" ; then
  echo "Committing and pushing..."
else
  echo "Cancelling"
  exit 1
fi

branchname="bump_version_to_${version}"

git checkout -b $branchname
git add gradle.properties
git add README.md
git commit -m "Bump version to ${version}"
git push origin $branchname

echo "Now submit a PR and get it submitted internally."
echo "Then tag the new commit with the new version (e.g. v4.10.0)."
echo "And upload them:"
echo "git push origin v4.10.0"
echo "git push bump v4.10.0"
echo "Then upload the build with:"
echo "./gradlew clean build --parallel"
echo "./gradlew uploadArchives"
