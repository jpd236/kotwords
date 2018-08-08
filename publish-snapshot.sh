#!/bin/bash
#
# Publish a SNAPSHOT build on any commit to master.
#
# Does nothing for other repositories and branches, or on pull requests.

SLUG="jpd236/kotwords"
BRANCH="master"

set -eu

if [ "$TRAVIS_REPO_SLUG" != "$SLUG" ]; then
    echo "Not publishing snapshot; wrong repository. Expected '$SLUG' but was '$TRAVIS_REPO_SLUG'."
elif [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
    echo "Not publishing snapshot; pull request."
elif [ "$TRAVIS_BRANCH" != "$BRANCH" ]; then
    echo "Not publishing snapshot; wrong branch. Expected '$BRANCH' but was '$TRAVIS_BRANCH'."
else
    echo "Publishing snapshot..."
    ./gradlew artifactoryPublish
    echo "Published snapshot to OJO!"
fi
