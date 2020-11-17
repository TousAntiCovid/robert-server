#!/bin/bash

# Utility functions (e.g. for performing requests)
# Most/all of these functions require TACW_BASE_URL to be set
TACW_BASE_URL=${TACW_BASE_URL:-"http://localhost:8080"}
VERSION=${VERSION:-"v1"}

# Register to the TAC server
# (out-of scope for the initial POC)
register () {
    true
}

# Send a report to ROBERT
# This will return a token that can be used
# to authenticate TAC-W report requests
# (out-of scope for the initial POC)
rreport () {
    true
}

# Perform a TAC-warning status query
wstatus () {
    curl --fail \
         --no-progress-meter \
         --header "Content-Type: application/json" \
         --request POST \
         --data "$1" \
         "${TACW_BASE_URL}"/api/tac-warning/"${VERSION}"/status
}

# Perform a TAC-warning visit report
wreport () {
    curl --fail \
         --no-progress-meter \
         --header "Content-Type: application/json" \
         --header "Authorization: Bearer $1" \
         --request POST \
         --data "$2" \
         "${TACW_BASE_URL}"/api/tac-warning/"${VERSION}"/report
}

extract_status_at_risk () {
    echo "$1" | jq .atRisk
}