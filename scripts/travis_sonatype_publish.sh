#!/bin/bash
# Publishes the latest snapshot to Sonatype. Works only for SNAPSHOT versions.
# Based closely on material from
# http://benlimmer.com/2014/01/04/automatically-publish-to-sonatype-with-gradle-and-travis-ci.

if [ "$SONATYPE_PUBLISH" == "true" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "$SONATYPE_PUBLISH_BRANCH" ]; then
  echo -e "Starting publish to Sonatype...\n"

  ./gradlew uploadArchives -PNEXUS_USERNAME="${NEXUS_USERNAME}" -PNEXUS_PASSWORD="${NEXUS_PASSWORD}"
  RETVAL=$?

  if [ $RETVAL -eq 0 ]; then
    echo 'Completed publish!'
  else
    echo 'Publish failed.'
    return 1
  fi
else
    echo 'Skipping Sonatype push'
fi
