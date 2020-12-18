#!/bin/bash
set -euo pipefail
source ./common.sh



#Initialise the UUID for the visited locations
ERP1=$(uuid)

echo "----Stacy registers"

get_captcha

echo "captcha is ${captchaContent}"
register "$(createRegister $captchaId $captchaContent)"
#status "@robert-status.json"
# Hugo [healthy] visits a restaurant yesterday at 12:30 and logs this visit on his phone
hugo_tk_list=""
hugo_visit_date=$($date +%s -d "1 day ago 12:30")
hugo_tk_list=$(createVisitToken "STATIC" "$(computeTokenPayload "$ERP1")" "$hugo_visit_date")
for i in {2..3}
do
  hugo_visit_date=$($date +%s -d "$i day ago 12:30")
  hugo_tk1=$(createVisitToken "STATIC" "$(computeTokenPayload "$ERP1")" "$hugo_visit_date")
  echo $hugo_tk1
  hugo_tk_list="$hugo_tk_list ,  $hugo_tk1"
done
# Stacy [sick] visits the same restaurant yesterday at 12:20
# and logs this visit on her phone
stacy_visit_date=$($date +%s -d "1 day ago 12:20")
stacy_visit1=$(createVisit "STATIC"  "$ERP1" "$stacy_visit_date")

# Hugo checks his status in the app, he will not be considered at risk yet
echo "----Hugo checks his status"
hugo_visitTokens=$(createVisitTokens "$hugo_tk_list")
hugo_first_check=$(wstatus "$hugo_visitTokens")
test_status_at_risk "$hugo_first_check" "false"

# Stacy performs a COVID test that comes back positive.
# The apps report to robert. Robert return a JWT used in the report to tac-warning
# She uploads her visit history to the server which hashes it
# for better privacy
echo "----Stacy performs a COVID test that comes back positive."
echo "enter the Token for ROBERT report"


qrcodeForReport=$(get_qrcode_from_user)

jwt=$(report "$(builtRoberReport "$qrcodeForReport")" | jq -e ".reportValidationToken" -r)

echo "jwt="
echo $jwt
echo
echo "Got JWT from Robert, now reporting to TACW"
wreport "$jwt" "$(createVisits "$stacy_visit1")"

# Later, Hugo performs another status check. It still comes back negative
# because the risk threshold has not been crossed.
echo "----Later, Hugo performs another status check. It still comes back negative because the risk threshold has not been crossed."
hugo_second_check=$(wstatus "$hugo_visitTokens")
test_status_at_risk "$hugo_second_check" "false"

echo '----done!'