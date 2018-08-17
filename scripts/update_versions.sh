#!/usr/bin/env bash

set -e

if [ "$#" -ne 2 ]; then
  echo "Usage: ./update_posts.sh <from major.minor.patch> <to major.minor.patch>"
  exit 1
fi

from=$1
to=$2
from_for_sed=$(echo $1 | sed 's/\./\\\./g')
to_for_sed=$(echo $2 | sed 's/\./\\\./g')

snapshot_next_version_minor=$(echo $to | cut -d '.' -f 2 | xargs expr 1 +)
snapshot_next_version="$(echo $to | cut -d '.' -f 1).${snapshot_next_version_minor}.$(echo $to | cut -d '.' -f 3)"

echo "updating versions from ${from} to ${to}"

# Update references from the old version to the new version in pages (outside of javadocs.md)
find _posts -type f -name '*.md' | grep -v "javadocs.md" | xargs sed -i '' "s/$from_for_sed/$to_for_sed/g"
# Update references to the new version SNAPSHOT to one more than the new version SNAPSHOT
find _posts -type f -name '*.md' | grep -v "javadocs.md" | xargs sed -i '' "s/${to}-SNAPSHOT/${snapshot_next_version}-SNAPSHOT/g"
