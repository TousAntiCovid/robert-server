#! /bin/bash
export LC_ALL=fr_FR.utf8
export LANG=fr_FR.utf8
export PATH=$PATH:~/.local/bin

SCRIPTPATH="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
cd $SCRIPTPATH

CLEA_BATCH_CLUSTER_OUTPUT_PATH=${CLEA_BATCH_CLUSTER_OUTPUT_PATH:-/tmp/v1}
mkdir -p ${CLEA_BATCH_CLUSTER_OUTPUT_PATH}

export FLASK_APP=web.py
export FLASK_ENV=development

flask run --host="0.0.0.0" --port 15000 