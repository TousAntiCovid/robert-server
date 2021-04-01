#!/bin/bash

PROGNAM=$(basename $0)
die() { echo "[$PROGNAM] $*" 1>&2 ; exit 1; }

WORKDIR=${CLEA_BATCH_CLUSTER_OUTPUT_PATH:-/tmp/v1}
BUCKET=${BUCKET:-cleacluster-eu-west-3}

set -o pipefail  # trace ERR through pipes
set -o errtrace  # trace ERR through 'time command' and other functions
set -o nounset   ## set -u : exit the script if you try to use an uninitialised variable
#set -o errexit   ## set -e : exit the script if any statement returns a non-true return value
set +e


if ! java -jar clea-batch.jar $@ ; then
    die "Java batch fails"
fi

# ============================================
# !!!!! MOCK !!!!!!
# ============================================
#GEN=$((1 + $RANDOM % 100))
#mkdir -p $WORKDIR/v1/$GEN
#for i in {1..11} ; do NEW_UUID=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 6 | head -n 1); touch $WORKDIR/v1/$GEN/$NEW_UUID.json; done
#touch /tmp/v1/$GEN/indexCluster.json
# ============================================

echo "[$PROGNAM] Coying files...."

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
s3cmd sync --exclude=clusterIndex.json $WORKDIR s3://${BUCKET}/ || die "S3cmd fails to copy cluster files to bucket"

# only indexCluster.json at the root of "v1"
s3cmd put $(find $WORKDIR -type f -name clusterIndex.json) s3://${BUCKET}/v1/ || die "S3cmd fails to copy clusterIndex file to bucket"

# purge batch temporary files
rm -rf $WORKDIR

