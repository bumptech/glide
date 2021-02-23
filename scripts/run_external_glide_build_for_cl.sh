#!/bin/bash
# This script uses Copybara to export Glide from a given cl number to a temp
# directory and then runs the external build on the exported version of Glide.
#
# If the build fails, the temporary directory containing the exported version
# of Glide will be printed. You can modify files within that temp directory and
# then retry the build as many times as you want using the command:
# 
# ./gradlew build -Dorg.gradle.java.home="/usr/local/buildtools/java/jdk8" \
#   --parallel
#
# This script does not publicly export any code, but instead dumps the export
# to a local temp folder. All of the temp folders start with
# glide_copybara_export_cl so they can be identified.
# 
# This script must be run from a google3 directory.

set -e
if [[ "$(basename "$(pwd)")" != "google3" ]]; then
  echo "Must be run from google3 root"
  exit 0
fi

if [ "$#" -ne 1 ]; then
  echo "Usage: ./run_external_glide_build_for_cl.sh <cl_number>"
  exit 0
fi

/google/data/ro/teams/copybara/copybara third_party/java_src/android_libs/glide/copy.bara.sky presubmit_piper_to_github $1 --dry-run --init-history --squash

tmp_dir=$(mktemp -d -t "glide_copybara_export_cl_$1_$(date +%Y-%m-%d-%H-%M-%S)_XXXXXXXXXX")
git clone /usr/local/google/home/judds/copybara/cache/git_repos/https%3A%2F%2Fgithub%2Ecom%2Fbumptech%2Fglide $tmp_dir

(cd $tmp_dir && ./gradlew build -Dorg.gradle.java.home="/usr/local/buildtools/java/jdk8" --parallel) \
  || (echo "Build failed, you can iterate on the build here: $tmp_dir" \
      && exit 0)

