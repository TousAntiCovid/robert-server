#! /bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

if [ $# -eq 0 ] ; then
   echo "Usage: $0 prefixe alt_name"
   echo "  Generate a certificate sign by an authority (generated first)"
   echo ""
   echo "Example: $0 kafka broker"
   echo ""
   exit 0
fi

PREFIXE=$1
PREFIXE=${PREFIXE:-kafka}

#format SUBJECT_ALTERNATIVE_NAME=dns:test.abc.com,ip:1.1.1.1
SUBJECT_ALTERNATIVE_NAME=$2
EXT_SAN="" # optional -ext SAN section
[ -n "$SUBJECT_ALTERNATIVE_NAME" ] && EXT_SAN="-ext SAN=dns:${SUBJECT_ALTERNATIVE_NAME}"

# don't execute dangerous rm ./* so remove certificats before changing folder  
rm certs/${PREFIXE}.* 2>/dev/null

mkdir -p $SCRIPTPATH/certs
pushd $SCRIPTPATH/certs

#- 1) create CA once 
if [ ! -f "ca.crt" ] ; then 
    #- Create a Certificate Authority. The generated CA is a public-private key pair and certificate used to sign other certificates. A CA is responsible for signing certificates.
    openssl req -new -newkey rsa:4096 -days 365 -x509 -subj "/CN=Kafka-Security-CA" -out ca.crt -keyout ca.key -nodes

    #- 5) Create a truststore by importing the CA public certificate so that the kafka broker is trusting all certificates which has been issued by our CA:
    keytool -keystore truststore.jks -alias CARoot -import -file ca.crt -storepass serversecret -keypass serversecret -noprompt
fi

#- 2) Create a kafka broker certificate:
echo keytool -genkey -keyalg RSA -keystore ${PREFIXE}.keystore.jks -validity 365 -storepass serversecret -keypass serversecret -alias ${PREFIXE} -dname "CN=${PREFIXE}" $EXT_SAN -storetype pkcs12
keytool -genkey -keyalg RSA -keystore ${PREFIXE}.keystore.jks -validity 365 -storepass serversecret -keypass serversecret -alias ${PREFIXE} -dname "CN=${PREFIXE}" $EXT_SAN -storetype pkcs12

#- 3) Get the signed version of the certificate:
keytool -keystore ${PREFIXE}.keystore.jks -certreq -alias ${PREFIXE} -file ${PREFIXE}.csr -storepass serversecret -keypass serversecret

#- 4) Sign the certificate with the CA:
openssl x509 -req -CA ca.crt -CAkey ca.key -in ${PREFIXE}.csr -out ${PREFIXE}.crt -days 365 -CAcreateserial -passin pass:serversecret


# - 6) Import the signed certificate in the keystore:
keytool -keystore ${PREFIXE}.keystore.jks -alias CARoot -import -file ca.crt -storepass serversecret -keypass serversecret -noprompt
keytool -keystore ${PREFIXE}.keystore.jks -alias ${PREFIXE} -import -file ${PREFIXE}.crt -storepass serversecret -keypass serversecret -noprompt

echo "serversecret" > ${PREFIXE}-key-file.txt
echo "serversecret" > ${PREFIXE}-keystore-file.txt
echo "serversecret" > truststore-key-file.txt
popd


#From kafka 2.0 onwards, host name verification of servers is enabled by default and the errors were logged because,
#the kafka hostname didnt match the certificate CN. If your hostname and certificate doesnt match,
#then you can disable the hostname verification by setting the property ssl.endpoint.identification.algorithm to empty string
#ssl.endpoint.identification.algorithm =

