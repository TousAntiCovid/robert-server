#!/bin/bash
echo "Generate keys..."
cd /work/config
keytool -genseckey -alias federation-key -keyalg AES -keysize 256 -keystore keystore.p12 -storepass 1234 -storetype PKCS12 -v
keytool -genseckey -alias key-encryption-key -keyalg AES -keysize 256 -keystore keystore.p12 -storepass 1234 -storetype PKCS12 -v
for i in {0..5};do keytool -genseckey -alias server-key-$(date --date="$i days" +%Y%m%d) -keyalg AES -keysize 192 -keystore keystore.p12 -storepass 1234 -storetype PKCS12 -v; done

echo "Check keys"
keytool -list -keystore keystore.p12 -storepass 1234

echo "Launch appli"
cd /opt/robert-crypto-server
java $JAVA_OPTS -jar /opt/robert-crypto-server/app.jar

# useful to debug image build
#. /run.sh
