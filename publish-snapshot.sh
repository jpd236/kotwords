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
  # TODO: Remove --no-configuration-cache once https://github.com/gradle/gradle/issues/22779 is resolved
  # See also: https://vanniktech.github.io/gradle-maven-publish-plugin/central/#uploading-with-manual-publishing
  ./gradlew publishToMavenCentral --no-configuration-cache
  echo "Published build!"
fi
