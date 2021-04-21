#!/usr/bin/env bash

PROGNAM=$(basename $0)
die() { echo "[$PROGNAM] $*" 1>&2 ; exit 1; }

WORKDIR=${CLEA_BATCH_CLUSTER_OUTPUT_PATH:-/tmp/v1}
BUCKET=${BUCKET:-}

set -o pipefail  # trace ERR through pipes
set -o errtrace  # trace ERR through 'time command' and other functions
set -o nounset   ## set -u : exit the script if you try to use an uninitialised variable
#set -o errexit   ## set -e : exit the script if any statement returns a non-true return value
set +e


[ -n "${BUCKET}" ] || die "Environment variable BUCKET required"

if ! java -jar clea-batch.jar $@ ; then
    die "Java batch fails"
fi


echo "[$PROGNAM] Coying files...."


# Test that output folder exists, computing NBFILES fails if folder doesn't exist
[ -d $WORKDIR ] || die "Working directory $WORKDIR not exists" 


# count that there is at least "n" cluster files (to not push empty list)
MIN_FILES=1
NBFILES=$(find $WORKDIR -type f | wc -l)
if [ $NBFILES  -lt $MIN_FILES ] ;then
    die "not enough clusterfiles to continue ($NBFILES  -lt $MIN_FILES)"
fi
NB_INDEX=$(find $WORKDIR -type f -name "clusterIndex.json" |wc -l)
if [ $NB_INDEX -eq 0 ] ; then
  die "No clusterIndex.json generated"
fi
if [ $NB_INDEX -gt 1 ] ; then
  die "Many clusterIndex.json found ($NB_INDEX), possible partial or failed batch already present"
fi

# Copy clusterfiles to s3
# =======================

# All files except indexCluster.json
s3cmd sync --acl-public --exclude=clusterIndex.json $WORKDIR s3://${BUCKET}/ || die "S3cmd fails to copy cluster files to bucket"

# only indexCluster.json at the root of "v1"
s3cmd put --acl-public $(find $WORKDIR -type f -name clusterIndex.json) s3://${BUCKET}/v1/ || die "S3cmd fails to copy clusterIndex file to bucket"

# purge batch temporary files
rm -rf $WORKDIR

