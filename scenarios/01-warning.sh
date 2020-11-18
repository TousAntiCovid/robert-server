#!/bin/bash
set -euo pipefail
source ./common.sh

echo "----Stacy registers"
register "@01-register-stacy.json"

# Hugo [healthy] visits a restaurant  and logs this visit on his phone

# Stacy [sick] visits the same restaurant and logs this visit on her phone

# Hugo checks his status in the app, he will not be considered at risk yet
echo "----Hugo checks his status"
hugo_first_check=$(wstatus "@01-visits-hugo-tokens.json")
test_status_at_risk "$hugo_first_check" "false"

# Stacy performs a COVID test that comes back positive.
# The apps report to robert. Robert return a JWT used in the report to tac-warning
# She uploads her visit history to the server which hashes it
# for better privacy
echo "----Stacy performs a COVID test that comes back positive."
jwt=$(report "@01-report-stacy.json" | jq -e ".token" -r)
echo "Got JWT from Robert, now reporting to TACW"
wreport "$jwt" "@01-visits-stacy.json"

# Later, Hugo performs another status check. This time, it comes back positive.
echo "----Later, Hugo performs another status check. This time, it comes back positive."
hugo_second_check=$(wstatus "@01-visits-hugo-tokens.json")
test_status_at_risk "$hugo_second_check" "true"

echo '----done!'
