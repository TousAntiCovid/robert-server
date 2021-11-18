#!/bin/bash
set -e

function register_app() {
  CAPTCHA_ID=$(curl --fail --silent --header "Content-Type: application/json" --request POST https://api-int.tousanticovid.gouv.fr/api/v5/captcha | jq -r .id)
  echo "Open https://api-int.tousanticovid.gouv.fr/api/v5/captcha/$CAPTCHA_ID/image"
  if command -v feh > /dev/null ; then
    feh "https://api-int.tousanticovid.gouv.fr/api/v5/captcha/$CAPTCHA_ID/image" &
    FEH_PID=$!
  fi
  echo -n "then type the image content for $1: "
  read -r CAPTCHA_SOLUTION
  kill -15 $FEH_PID

  export "$1_CAPTCHA_ID=$CAPTCHA_ID"

  cat - <<EOR | curl --fail --silent --header "Content-Type: application/json" -X POST --data @- http://localhost:8180/api/v1/emulator/register
{
  "captchaId": "$CAPTCHA_ID",
  "captcha": "$CAPTCHA_SOLUTION"
}
EOR
}

function status() {
  echo -n " - status for user with captcha id $1: "
  sleep 2s
  echo -n "riskLevel="
  curl --fail --silent -X POST "http://localhost:8180/api/v1/emulator/status?captchaId=$1" | jq -r .riskLevel || echo fail
}

# Register 4 new virtual applications
register_app FRANCOIS
register_app SARAH
register_app PAUL
register_app HELENE
# or reuse previously registed applications
#FRANCOIS_CAPTCHA_ID=
#HELENE_CAPTCHA_ID=
#SARAH_CAPTCHA_ID=
#PAUL_CAPTCHA_ID=

cat - <<EOHELP

Here are the captcha ids used to register emulated mobile applications:
$(env | grep _CAPTCHA_ID)

You can reuse them to replace the previous \`register_app SOMEONE\`.

EOHELP

echo "Exchange HelloMessages between FRANCOIS and SARA..."
cat - <<EOR | curl --silent --header "Content-Type: application/json" -X POST --data @- http://localhost:8180/api/v1/emulator/helloMessageExchanges
{
  "captchaId": "$FRANCOIS_CAPTCHA_ID",
  "frequencyInSeconds": 1,
  "captchaIdOfOtherApps": [ "$SARAH_CAPTCHA_ID" ]
}
EOR

sleep 45m

echo "Stop HelloMessage exchanges"
curl --fail --silent -X DELETE "http://localhost:8180/api/v1/emulator/helloMessageExchanges?captchaId=$FRANCOIS_CAPTCHA_ID"

echo "Everyone executes a /status"
for captcha_id in $FRANCOIS_CAPTCHA_ID $SARAH_CAPTCHA_ID $PAUL_CAPTCHA_ID $HELENE_CAPTCHA_ID ; do
  status "$captcha_id"
done

echo "Obtain a submission code running"
echo "    curl localhost:8080/api/v1/generate/short"
echo "on one of the submission-code-server instances."
echo -n "Type the short code: "
read -r DECLARATION_CODE

cat - <<EOR | curl --fail --silent --header "Content-Type: application/json" -X POST --data @- http://localhost:8180/api/v1/emulator/report
{
  "captchaId": "$FRANCOIS_CAPTCHA_ID",
  "qrCode": "$DECLARATION_CODE"
}
EOR

echo "Run the robert-batch"
echo "    systemctl start robert-server.batch"
echo -n "Confirm when batch has been successfully executed"
read -r WAIT_CONFIRMATION

echo "Everyone executes a /status"
for captcha_id in $FRANCOIS_CAPTCHA_ID $SARAH_CAPTCHA_ID $PAUL_CAPTCHA_ID $HELENE_CAPTCHA_ID ; do
  status "$captcha_id"
done
