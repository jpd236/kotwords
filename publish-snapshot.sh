#!/bin/bash
#
# Publish a SNAPSHOT build on any commit to master.
#
# Does nothing for other repositories and branches, or on pull requests.

REPOSITORY="jpd236/kotwords"
BRANCH="master"

set -eu

if [ "$GITHUB_REPOSITORY" != "$REPOSITORY" ]; then
  echo "Not publishing snapshot; wrong repository. Expected '$REPOSITORY' but was '$GITHUB_REPOSITORY'."
elif [ "$GITHUB_PULL_REQUEST" != "false" ]; then
  echo "Not publishing snapshot; pull request."
elif [ "$GITHUB_BRANCH" != "$BRANCH" ]; then
  echo "Not publishing snapshot; wrong branch. Expected '$BRANCH' but was '$GITHUB_BRANCH'."
else
  echo "Publishing snapshot..."
  ./gradlew artifactoryPublish
  echo "Published snapshot to OJO!"
fi
