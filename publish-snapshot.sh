#!/bin/bash
#
# Publish a build on any commit to master.
#
# Does nothing for other repositories and branches, or on pull requests.

REPOSITORY="jpd236/kotwords"
BRANCH="master"

set -eu

GITHUB_BRANCH=${GITHUB_REF#refs/heads/}

if [ "$GITHUB_REPOSITORY" != "$REPOSITORY" ]; then
  echo "Not publishing snapshot; wrong repository. Expected '$REPOSITORY' but was '$GITHUB_REPOSITORY'."
elif [ "$GITHUB_EVENT_NAME" != "push" ]; then
  echo "Not publishing snapshot; pull request."
elif [ "$GITHUB_BRANCH" != "$BRANCH" ]; then
  echo "Not publishing snapshot; wrong branch. Expected '$BRANCH' but was '$GITHUB_BRANCH'."
else
  echo "Publishing snapshot..."
  # TODO: publishToMavenLocal should be unnecessary. Trying it as a workaround to ensure all
  # artifacts are built before any are published, since requests that come from multiple IPs
  # get split into different staging repositories and can't be merged, and perhaps this is more
  # likely when the requests are further apart.
  ./gradlew publishToMavenLocal
  ./gradlew publishToSonatype closeSonatypeStagingRepository
  echo "Published build!"
fi
