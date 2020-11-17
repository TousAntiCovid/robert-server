#!/bin/bash
set -euo pipefail

source ./common.sh

# Hugo [healthy] visits a restaurant and logs this visit on his phone

# Stacy [sick] visits the same restaurant and logs this visit on her phone

# Hugo checks his status in the app, will not be at risk yet
wstatus "@01-visits-hugo-tokens.json"

# Stacy performs a COVID test that comes back positive. 
# She uploads her visit history to the server which hashes it
# for better privacy
wreport "g00d4utht0k3n" "@01-visits-stacy.json"

# Later, Hugo performs another status check. This time, it comes back positive.
wstatus "@01-visits-hugo-tokens.json"