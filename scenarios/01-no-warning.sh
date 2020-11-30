#!/bin/bash
set -euo pipefail
source ./common.sh



#Initialise the UUID for the visited locations
ERP1=$(uuid)

echo "----Stacy registers"
register "@01-register-stacy.json"

# Hugo [healthy] visits a restaurant yesterday at 12:30 and logs this visit on his phone
hugo_visit_date=$($date +%s -d "1 day ago 12:30")
hugo_tk1=$(createVisitToken "STATIC" "$(computeTokenPayload "$ERP1")" "$hugo_visit_date")

# Stacy [sick] visits the same restaurant yesterday at 12:20
# and logs this visit on her phone
stacy_visit_date=$($date +%s -d "1 day ago 12:20")
stacy_visit1=$(createVisit "STATIC"  "$ERP1" "$stacy_visit_date")

# Hugo checks his status in the app, he will not be considered at risk yet
echo "----Hugo checks his status"
hugo_visitTokens=$(createVisitTokens "$hugo_tk1")
hugo_first_check=$(wstatus "$hugo_visitTokens")
test_status_at_risk "$hugo_first_check" "false"

# Stacy performs a COVID test that comes back positive.
# The apps report to robert. Robert return a JWT used in the report to tac-warning
# She uploads her visit history to the server which hashes it
# for better privacy
echo "----Stacy performs a COVID test that comes back positive."
jwt=$(report "@empty-robert-report.json" | jq -e ".token" -r)
echo "Got JWT from Robert, now reporting to TACW"
wreport "$jwt" "$(createVisits "$stacy_visit1")"

# Later, Hugo performs another status check. It still comes back negative
# because the risk threshold has not been crossed.
echo "----Later, Hugo performs another status check. It still comes back negative because the risk threshold has not been crossed."
hugo_second_check=$(wstatus "$hugo_visitTokens")
test_status_at_risk "$hugo_second_check" "false"

echo '----done!'
