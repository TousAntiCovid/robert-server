#!/usr/bin/env bash

if ! [[ -z "${CI_COMMIT_TAG}" ]]; then
	# do not execute this script if we are builing a TAG => we keep the version in the pom unmodified
	exit 0
fi

VERSION=$(mvn --non-recursive help:evaluate -Dexpression=project.version -q -DforceStdout)
mvn versions:set -DnewVersion=$VERSION-$CI_PIPELINE_IID -DprocessAllModules=true
