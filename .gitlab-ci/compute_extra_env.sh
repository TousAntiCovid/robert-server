#!/bin/sh
# script that computes some extra environment variables in order to simplify the main gitlab-ci.yml


# create a DOCKER_TAG  based on branch name
if [[ $CI_COMMIT_BRANCH = "develop" ]] 
then 
   export DOCKER_TAG=latest
else
   if [[ !  -z $CI_COMMIT_BRANCH ]] 
   then
      export DOCKER_TAG=ci-transient-$CI_COMMIT_BRANCH
   else
      export DOCKER_TAG=ci-transient
   fi
fi