#!/bin/bash
#
# Usage: ./scripts/update_javadocs.sh
#
# The version name is pulled automatically from gradle.properties.
set -e
set -o pipefail

TEMP_DIR="/tmp/tmp_glide_javadoc"
JAVADOC_GH_PAGES_DIR="javadocs"

major_version=$(fgrep VERSION_MAJOR gradle.properties | cut -d '=' -f 2)
minor_version=$(fgrep VERSION_MINOR gradle.properties | cut -d '=' -f 2)
version="${major_version}${minor_version}0"

echo "Updating javadocs for ${version}"

if [[ $(git status -uno --porcelain) ]];
then
  echo "One or more changes, commit or revert first."
  git status -uno --porcelain
  exit 1
fi

if [ -e "$JAVADOC_GH_PAGES_DIR" ];
then
  echo "javadocs directory exists locally, remove first."
  exit 1
fi

if [[ $(git rev-list master...origin/master --count) -ne 0 ]];
then
  echo "Origin and master are not up to date"
  git rev-list master...origin/master --pretty
  exit 1
fi
if [[ $(git rev-list gh-pages...origin/gh-pages --count) -ne 0 ]];
then
  echo "Origin and gh-pages are not up to date"
  git rev-list gh-pages...origin/gh-pages --pretty
  exit 1
fi

git checkout master
GIT_COMMIT_SHA="$(git rev-parse HEAD)"
./gradlew dokkaHtmlMultiModule
rm -rf $TEMP_DIR
cp -r build/dokka/htmlMultiModule $TEMP_DIR

git checkout gh-pages
rm -rf "${JAVADOC_GH_PAGES_DIR}/${version}"
cp -r $TEMP_DIR $JAVADOC_GH_PAGES_DIR/$version
rm -rf $TEMP_DIR
git add "${JAVADOC_GH_PAGES_DIR}/$version"
git commit -m "Update javadocs for version $version" -m "Generated from commit on master branch: ${GIT_COMMIT_SHA}"
echo "Copied javadoc into ${JAVADOC_GH_PAGES_DIR}/${version} and committed"
git log -1 --pretty=%B
echo "Ready to push"
