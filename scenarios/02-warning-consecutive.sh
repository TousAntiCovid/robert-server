#!/bin/bash
set -euo pipefail
source ./common.sh



#Initialise the UUID for the visited locations
ERP1=$(uuid)

echo "----Stacy registers"

get_captcha

echo "captcha is ${captchaContent}"
register "$(createRegister $captchaId $captchaContent)"
# Heather [healthy] visits a restaurant yesterday at 12:30 and logs this visit on his phone
heather_visit_date=$($date +%s -d "1 day ago 12:30")
heather_tk1=$(createVisitToken "STATIC" "$(computeTokenPayload "$ERP1")" "$heather_visit_date")
heather=$(createVisit "STATIC"  "$ERP1" "$heather_visit_date")

# Stephen [sick] visits the same restaurant yesterday at 11:15
# and logs this visit on her phone
stephen_visit_date=$($date +%s -d "1 day ago 11:15")
stephen_tk1=$(createVisitToken "STATIC" "$(computeTokenPayload "$ERP1")" "$stephen_visit_date")
stephen_visit1=$(createVisit "STATIC"  "$ERP1" "$stephen_visit_date")

# Serena [sick] visits the same restaurant yesterday at 13:30
# and logs this visit on her phone
serena_visit_date=$($date +%s -d "1 day ago 13:30")
serena_tk1=$(createVisitToken "STATIC" "$(computeTokenPayload "$ERP1")" "$serena_visit_date")
serena_visit1=$(createVisit "STATIC"  "$ERP1" "$serena_visit_date")

# Heather checks her status in the app, she will not be considered at risk yet
echo "----heather checks her status"
heather_visitTokens=$(createVisitTokens "$heather_tk1")
heather_first_check=$(wstatus "$heather_visitTokens")
test_status_risk_level "$heather_first_check" "0"

# Stephen performs a COVID test that comes back positive.
# The apps report to robert. Robert return a JWT used in the report to tac-warning
# She uploads her visit history to the server which hashes it
# for better privacy
echo "----Stephen performs a COVID test that comes back positive."
qrcodeForReport=$(get_qrcode_from_user)
reportJson=$(builtRoberReport "$qrcodeForReport")
echo $reportJson
jwt=$(report "$(builtRoberReport "$qrcodeForReport")" | jq -e ".reportValidationToken" -r)

echo "Got JWT from stephen, now reporting to TACW"
wreport "$jwt" "$(createVisits "$stephen_visit1")"

# Serena performs a COVID test that comes back positive.
# The apps report to robert. Robert return a JWT used in the report to tac-warning
# She uploads her visit history to the server which hashes it
# for better privacy
echo "----Serena performs a COVID test that comes back positive."
qrcodeForReport=$(get_qrcode_from_user)
reportJson=$(builtRoberReport "$qrcodeForReport")
echo $reportJson
jwt=$(report "$(builtRoberReport "$qrcodeForReport")" | jq -e ".reportValidationToken" -r)

echo "Got JWT from serena, now reporting to TACW"
wreport "$jwt" "$(createVisits "$serena_visit1")"

# Later, Heather performs another status check. It comes back positive because the risk threshold has been crossed.
echo "----Later, heather performs another status check. It comes back positive because the risk threshold has been crossed."
heather_second_check=$(wstatus "$heather_visitTokens")
test_status_risk_level "$heather_second_check" "3"

echo '----done!'
