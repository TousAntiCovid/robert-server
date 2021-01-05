#!/bin/bash

# Utility functions (e.g. for performing requests)
# Most/all of these functions require TACW_BASE_URL to be set
ROBERT_BASE_URL=${ROBERT_BASE_URL:-"http://localhost:8086/api/"}

ROBERT_VERSION=${ROBERT_VERSION:-"v4"}
CAPTCHA_ROBERT_VERSION=${CAPTCHA_ROBERT_VERSION:-"v2"}
TACW_BASE_URL=${TACW_BASE_URL:-"http://localhost:8080/api/tac-warning"}

TACW_VERSION=${TACW_VERSION:-"v2"}
SALT_RANGE=2
TIME_ROUNDING=900
NUMBER_OF_VISITS_TO_REPORT=5
USE_CAPTCHA=1
NB_OF_RETENTION_DAY=12
READ_REPORT_QR_CODE_FROM_USER=1

CURL_NO_PROGRESS_OPTION=--no-progress-meter

unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)
        date="date";;
    Darwin*)
        date="/usr/local/bin/gdate"
        CURL_NO_PROGRESS_OPTION="--show-error --silent";;
    CYGWIN*)
        machine=Cygwin;;
    MINGW*)
        machine=MinGw;;
    *)
        machine="UNKNOWN:${unameOut}"
esac

# Register to the TAC server
register () {
    curl -k --fail \
         $CURL_NO_PROGRESS_OPTION \
         --header "Content-Type: application/json" \
         --request POST \
         --data "$1" \
         "${ROBERT_BASE_URL}/${ROBERT_VERSION}"/register
}

# Status to the TAC server
status () {
    curl -k --fail \
         $CURL_NO_PROGRESS_OPTION \
         --header "Content-Type: application/json" \
         --request POST \
         --data "$1" \
         "${ROBERT_BASE_URL}/${ROBERT_VERSION}"/status
}

captcha () {
    curl -k --fail \
         $CURL_NO_PROGRESS_OPTION \
         --header "Content-Type: application/json" \
         --request POST \
         "${ROBERT_BASE_URL}/${CAPTCHA_ROBERT_VERSION}"/captcha
}

# Send a report to ROBERT
# This will return a token that can be used
# to authenticate TAC-W report requests
report () {
    curl -k --fail \
         $CURL_NO_PROGRESS_OPTION \
         --header "Content-Type: application/json" \
         --request POST \
         --data "$1" \
         "${ROBERT_BASE_URL}/${ROBERT_VERSION}"/report
}

# Perform a TAC-warning status query
wstatus () {
    curl -k --fail \
         $CURL_NO_PROGRESS_OPTION \
         --header "Content-Type: application/json" \
         --request POST \
         --data "$1" \
         "${TACW_BASE_URL}/${TACW_VERSION}"/wstatus
}

# Perform a TAC-warning visit report
wreport () {
    curl -k --fail \
         $CURL_NO_PROGRESS_OPTION \
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

# arg 1 = token
builtRoberReport(){
 echo  '{  "token": "'$1'",  "contacts": []}' | jq .
}

# arg 1 = captcha id
# arg 2 : captcha text
createRegister(){
  echo  ' {"captcha": "'$2'",  "captchaId": "'$1'",  "clientPublicECDHKey": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtLhNO6Ez2Gc6H+xHCKUgVAOYk5PzQbcoNPxVvsE8IIHLQIoMlj9sj3A4oEHv8Ke/9xm9h6phSDkmficc24gJ+Q==",
  "pushInfo": {    "locale": "fr",    "timezone": "Europe/Paris",  "token": "string" }}'| jq .
}
# arg 1 = STATIC or DYNAMIC
# arg 2 = erp
# arg 3 = absolute date **as a UNIX timestamp (seconds since 1970-01-01)**
# For instance, the output of running `date +%s -d "2 days ago 2:30pm"`
createVisit(){
  ntptime=$(unix_time_to_ntp_time "$3")
  echo '{ "timestamp": "'$(roundedntptimestamp $ntptime)'", "qrCode": { "type": "'$1'","venueCategory": 3, "venueType": "T", "venueCapacity": 42, "uuid": "'$2'"  } }' | jq .
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

get_captcha(){
  echo "--Get captcha"
  if [ "1" = "$USE_CAPTCHA" ]; then
    captchaId=$(captcha  | jq -e ".id" -r)
    echo "${ROBERT_BASE_URL}/${CAPTCHA_ROBERT_VERSION}/captcha/${captchaId}/image"
    open "${ROBERT_BASE_URL}/${CAPTCHA_ROBERT_VERSION}/captcha/${captchaId}/image"
    echo "Text of the captcha :"
    read captchaContent
  else
    captchaContent="string"
    captchaId="600d6f41ef7e4048a04ca6baa2405270"
  fi
}

get_qrcode_from_user(){
    if [ "1" = "$READ_REPORT_QR_CODE_FROM_USER" ]; then
    read qrcode
  else
    qrcode=$(uuid)
  fi
  echo $qrcode
}
