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
# Revert the reference in configuration back to 4.9.0
sed -i '' "s/Starting in Glide ${to_for_sed}/Starting in Glide 4.9.0/" _posts/2017-03-14-configuration.md
# Update references to the new version SNAPSHOT to one more than the new version SNAPSHOT
find _posts -type f -name '*.md' | grep -v "javadocs.md" | xargs sed -i '' "s/${to}-SNAPSHOT/${snapshot_next_version}-SNAPSHOT/g"

# Update the javadocs page
# First grab the next reference number:
next_reference_number=$(sed -E "s:^\[([0-9]*)\].*:\1:" _posts/2015-05-17-javadocs.md | grep -e "^[0-9]" | tail -1 | xargs expr 1 +)
# Then prepend a line with the new snapshot version
echo _posts/2015-05-17-javadocs.md | xargs sed -i '' '/-SNAPSHOT/i \
* [Glide '"${snapshot_next_version}-SNAPSHOT][${next_reference_number}]
"
# Then remove the -SNAPSHOT from the old version
sed -i '' "s/${to_for_sed}-SNAPSHOT/${to_for_sed}/" _posts/2015-05-17-javadocs.md
# And append the correct reference with the new versions javadoc directory.
echo "[${next_reference_number}]:{{ site.baseurl }}{% link /javadocs/$(echo $snapshot_next_version | sed 's/\.//g')/index.html %}" >> _posts/2015-05-17-javadocs.md
