#!/bin/bash

# Utility functions (e.g. for performing requests)
# Most/all of these functions require TACW_BASE_URL to be set
ROBERT_BASE_URL=${ROBERT_BASE_URL:-"http://localhost:8086/api/"}

ROBERT_VERSION=${ROBERT_VERSION:-"v3"}
TACW_BASE_URL=${TACW_BASE_URL:-"http://localhost:8080/api/tac-warning"}

TACW_VERSION=${TACW_VERSION:-"v1"}
SALT_RANGE=1000
TIME_ROUNDING=900


unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)     date="date";;
    Darwin*)    date="/usr/local/bin/gdate";;
    CYGWIN*)    machine=Cygwin;;
    MINGW*)     machine=MinGw;;
    *)          machine="UNKNOWN:${unameOut}"
esac

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
         "${TACW_BASE_URL}/${TACW_VERSION}"/wstatus
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
         "${TACW_BASE_URL}/${TACW_VERSION}"/wreport
}

test_status_at_risk () {
    # jq -e exits with a nonzero error when the last boolean
    # comparison is false
   echo "$1" |  jq  -e ".atRisk==$2"

}


# arg: unix timestamp in seconds
unix_time_to_ntp_time(){
  (( ntptime = $1 + (70*365+18)*86400 ))
  echo "$ntptime"
}

# arg 1: STATIC or DYNAMIC
# arg 2: token
# arg 3: absolute date **as a UNIX timestamp (seconds since 1970-01-01)**
# For instance, the output of running `date +%s -d "2 days ago 2:30pm"`
createVisitToken(){
  ntptime=$(unix_time_to_ntp_time "$3")
  roundedntpts=$(roundedntptimestamp "$ntptime")
  echo '{    "type" : "'$1'",    "payload" : "'$2'",   "timestamp" : "'$roundedntpts'"}'
}

# arg: tokens
createVisitTokens(){
  echo '{"visitTokens" : [ '$1' ]}' | jq .
}

# arg 1 = STATIC or DYNAMIC
# arg 2 = erp
# arg 3 = absolute date **as a UNIX timestamp (seconds since 1970-01-01)**
# For instance, the output of running `date +%s -d "2 days ago 2:30pm"`
createVisit(){
  ntptime=$(unix_time_to_ntp_time "$3")
  echo '{ "timestamp": "'$(roundedntptimestamp $ntptime)'", "qrCode": { "type": "'$1'", "venueType": "N", "venueCapacity": 42, "uuid": "'$2'"  } }' | jq .
}

# arg: visits
createVisits(){
  echo '{"visits" : [ '$1' ]}' | jq .
}

# Creates a visit token
# arg : venue UUID
computeTokenPayload(){
  string=$(( ($RANDOM % SALT_RANGE) + 1))$1
  h=($(echo -n $string | shasum -a 256)) ;
  echo $h
}

# arg NTP timestamp
roundedntptimestamp(){
  ntpts="$1"
  (( halfrange = TIME_ROUNDING / 2 ))
  # shellcheck disable=SC2004
  (( rest = $ntpts % TIME_ROUNDING ))
  # shellcheck disable=SC2004
  if (( rest > halfrange )); then
    (( rntpts = ntpts + halfrange - rest))
  else
    ((rntpts = ntpts - rest ))
  fi
  echo $rntpts
}
