#!/bin/bash

# Utility functions (e.g. for performing requests)
# Most/all of these functions require TACW_BASE_URL to be set
ROBERT_BASE_URL=${ROBERT_BASE_URL:-"http://localhost:8086/api/"}
ROBERT_VERSION=${ROBERT_VERSION:-"v3"}
TACW_BASE_URL=${TACW_BASE_URL:-"http://localhost:8080/api/tac-warning"}
TACW_VERSION=${TACW_VERSION:-"v1"}
SALT_RANGE=1000
TIME_ROUNDING=3600
ERP1=`uuid`
ERP2=`uuid`
ERP3=`uuid`

# Register to the TAC server
register () {
    # TODO --no-progress-meter is not a valid option on OS X => use --silent instead
    curl --fail \
         --no-progress-meter \
         --header "Content-Type: application/json" \
         --request POST \
         --data "$1" \
         "${ROBERT_BASE_URL}/${ROBERT_VERSION}"/register
}

# Send a report to ROBERT
# This will return a token that can be used
# to authenticate TAC-W report requests
report () {
    # TODO --no-progress-meter is not a valid option on OS X => use --silent instead
    curl --fail \
         --no-progress-meter \
         --header "Content-Type: application/json" \
         --request POST \
         --data "$1" \
         "${ROBERT_BASE_URL}/${ROBERT_VERSION}"/report
}

# Perform a TAC-warning status query
wstatus () {
    # TODO --no-progress-meter is not a valid option on OS X => use --silent instead
    curl --fail \
         --no-progress-meter \
         --header "Content-Type: application/json" \
         --request POST \
         --data "$1" \
         "${TACW_BASE_URL}/${TACW_VERSION}"/status
}

# Perform a TAC-warning visit report
wreport () {
    # TODO --no-progress-meter is not a valid option on OS X => use --silent instead
    curl --fail \
         --no-progress-meter \
         --header "Content-Type: application/json" \
         --header "Authorization: Bearer $1" \
         --request POST \
         --data "$2" \
         "${TACW_BASE_URL}/${TACW_VERSION}"/report
}

test_status_at_risk () {
    # jq -e exits with a nonzero error when the last boolean
    # comparison is false
   echo "$1" |  jq  -e ".atRisk==$2"

}

getntptimestamp(){
  now=`date +%s`
#  (( ntpts = $now + (70*365+18)*86400 ))
  (( ntpts = $now  ))
  echo $ntpts
}

# arg 1 = STATIC or DYNAMIC
# arg 2 = token
createVisitToken(){
#  jq -n --arg type $1 --arg payload $2 '"visitTokens" \: [ {    "type" : $type,    "payload" : $payload}]}'
  echo '{    "type" : "'$1'",    "payload" : "'$2'"}'
}

#arg tokens
createVisitTokens(){
#  jq -n --arg type $1 --arg payload $2 '"visitTokens" \: [ {    "type" : $type,    "payload" : $payload}]}'
  echo '{"visitTokens" : [ '$1' ]}' | jq .
}

# arg 1 = STATIC or DYNAMIC
# arg 2 = erp
# arg 3 = offset in day
createVisit(){
#  jq -n --arg type $1 --arg payload $2 '"visitTokens" \: [ {    "type" : $type,    "payload" : $payload}]}'
  echo '{ "timestamp": "'$(roundedntptimestamp $3)'", "qrCode": { "type": "'$1'", "venueType": "N", "venueCapacity": 42, "uuid": "'$2'"  } }'
}

#arg visits
createVisits(){
#  jq -n --arg type $1 --arg payload $2 '"visitTokens" \: [ {    "type" : $type,    "payload" : $payload}]}'
  echo '{"visits" : [ '$1' ]}' | jq .
}

# Creates a visit token
# arg offset in day
# arg ERP UUID
computeTokenPayload(){
  string=$(( $RANDOM % SALT_RANGE ))$(roundedntptimestamp $1)$2
  h=($(echo -n $string | shasum )) ;
  echo $h
}

# arg offsetInDays
roundedntptimestamp(){
  ntpts=`date +%s`
  (( ntpts = ntpts - $1 *86400))
  (( halfrange = TIME_ROUNDING / 2 ))
  # shellcheck disable=SC2004
  (( rest = $ntpts % TIME_ROUNDING ))
  # shellcheck disable=SC2004
  if (( rest > halfrange )); then
    (( rntpts = ntpts + halfrange - rest))
  else
    ((rntpts = ntpts - rest ))
  fi
  ((rntpts = rntpts  + (70*365+18)*86400  ))
  echo $rntpts
}