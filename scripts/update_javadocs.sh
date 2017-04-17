#!/bin/bash
set -e
set -o pipefail

TEMP_DIR="/tmp/tmp_glide_javadoc"
JAVADOC_GH_PAGES_DIR="javadocs"

if [[ -z "$1" ]]; 
then
  echo "You must supply a target version"
  echo "Usage ./scripts/update_javadocs.sh <400>"
  exit 1
fi

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
./gradlew clean releaseJavadocJar javadoc
rm -rf $TEMP_DIR
cp -r glide/build/docs/javadoc $TEMP_DIR
git checkout gh-pages
rm -rf "${JAVADOC_GH_PAGES_DIR}/${1}"
cp -r $TEMP_DIR $JAVADOC_GH_PAGES_DIR/$1
rm -rf $TEMP_DIR
git add "${JAVADOC_GH_PAGES_DIR}/$1" 
git commit -m "Update javadocs for version $1" -m "Generated from commit on master branch: ${GIT_COMMIT_SHA}"
echo "Copied javadoc into ${JAVADOC_GH_PAGES_DIR}/${1} and committed"
git log -1 --pretty=%B
echo "Ready to push"
