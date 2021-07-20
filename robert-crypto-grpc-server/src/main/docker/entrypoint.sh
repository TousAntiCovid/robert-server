#!/bin/sh
echo "Generate keys..."
python3 /hsm-tools/key-management/init-from-scratch/populate-hsm_ks_days.py -p "1234" -t "StopCovid" -n "90"

echo "Check keys"
pkcs11-tool --module /usr/local/lib/softhsm/libsofthsm2.so -l -p "1234" -O

echo "Launch appli"
cd /opt/robert-crypto-server
java $JAVA_OPTS -Dlogging.config=/work/config/logback.xml -jar /opt/robert-crypto-server/app.jar --spring.config.location=file:/work/config/application.properties

# useful to debug image build
#. /run.sh