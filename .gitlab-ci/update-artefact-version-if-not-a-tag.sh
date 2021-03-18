#!/usr/bin/env bash

VERSION=$(mvn --non-recursive help:evaluate -Dexpression=project.version -q -DforceStdout)

if ! [[ -z "${CI_COMMIT_TAG}" ]]; then
	# do not execute this script if we are builing a TAG => we keep the version in the pom unmodified
	echo $VERSION
fi

VERSION=$VERSION-$CI_PIPELINE_IID
mvn versions:set -DnewVersion=$VERSION -DprocessAllModules=true

echo $VERSION