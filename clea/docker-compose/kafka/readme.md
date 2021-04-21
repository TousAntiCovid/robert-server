# GOAL 

Generate a "root" certificate then a certificate for Kafka.


# Instructions

## Root CA

Generate a private key, then a certificate
```sh
openssl genrsa -out ca.pem 2048

openssl req -new -x509 -key ca.pem -out ca.crt -days 1095
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:FR
State or Province Name (full name) [Some-State]:
Locality Name (eg, city) []:
Organization Name (eg, company) [Internet Widgits Pty Ltd]:inria
Organizational Unit Name (eg, section) []:INT
Common Name (e.g. server FQDN or YOUR name) []:root-int
Email Address []:
```

## Kafka Int certificats

Generate a private key, then a certificate request
```sh
openssl genrsa -out kafka-int.pem 2048

openssl req -new -key kafka-int.pem -out kafka-int.csr
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:FR
State or Province Name (full name) [Some-State]:
Locality Name (eg, city) []:
Organization Name (eg, company) [Internet Widgits Pty Ltd]:inria
Organizational Unit Name (eg, section) []:INT
Common Name (e.g. server FQDN or YOUR name) []:kafka-int
Email Address []:

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
An optional company name []:

openssl x509 -req -in kafka-int.csr -out kafka-int.pem -CA ca.crt -CAkey ca.pem -CAcreateserial -CAserial ca.srl -days 1095
Signature ok
subject=C = FR, ST = Some-State, O = inria, OU = INT, CN = kafka-int
Getting CA Private Key
```

## Kafka-int keystore / Trustore

The keystore will be used by Kafka to initialize TLS

The trustore contains ca.crt provide to Application to validate Kafka certificat.


TO create the keystore : 
1°) transform crt+key to p12 format, including the chain (root certificat)

```sh
openssl pkcs12 -export -in kafka-int.crt -inkey kafka-int.pem -chain -CAfile ca.crt -name "kafka" -out kafka-int.p12
Enter Export Password: changeit
Verifying - Enter Export Password: changeit

keytool -importkeystore -deststorepass keystore-pass -destkeystore kafka-int.jks -srckeystore kafka-int.p12 -srcstoretype PKCS12
Import du fichier de clés kafka-int.p12 vers kafka-int.jks...
Entrez le mot de passe du fichier de clés source :  
L''entrée de l''alias kafka a été importée.
Commande d''import exécutée : 1 entrées importées, échec ou annulation de 0 entrées
```


To create a trustore :


```sh
$ keytool -keystore truststore.jks -alias root-ca -import -file ca.crt
Entrez le mot de passe du fichier de clés :  truststore-pass
Ressaisissez le nouveau mot de passe : truststore-pass
Propriétaire : CN=root-int, OU=INT, O=inria, ST=Some-State, C=FR
Emetteur : CN=root-int, OU=INT, O=inria, ST=Some-State, C=FR
Numéro de série : 6f209b294c81263d8ebfd7565c997d79dc4c38e8
Valide du Wed Mar 31 10:49:09 CEST 2021 au Sat Mar 30 09:49:09 CET 2024
Empreintes du certificat :
	 SHA 1: 3C:70:61:45:49:04:B2:B3:79:19:90:04:46:53:4F:DD:3F:A8:1E:65
	 SHA 256: 95:70:35:06:9E:C7:3D:5E:8A:1C:BB:DC:73:AB:E3:EA:E0:18:A3:36:C9:4E:44:1A:DB:67:10:0C:25:6F:DE:DD
Nom de l''algorithme de signature : SHA256withRSA
Algorithme de clé publique du sujet : Clé RSA 2048 bits
Version : 3

Extensions : 

#1: ObjectId: 2.5.29.35 Criticality=false
AuthorityKeyIdentifier [
KeyIdentifier [
0000: 4A B8 E6 A7 4A 51 98 60   09 7C 0F 7B 77 69 C4 32  J...JQ.....wi.2
0010: 82 62 EA 85                                        .b..
]
]

#2: ObjectId: 2.5.29.19 Criticality=true
BasicConstraints:[
  CA:true
  PathLen:2147483647
]

#3: ObjectId: 2.5.29.14 Criticality=false
SubjectKeyIdentifier [
KeyIdentifier [
0000: 4A B8 E6 A7 4A 51 98 60   09 7C 0F 7B 77 69 C4 32  J...JQ.....wi.2
0010: 82 62 EA 85                                        .b..
]
]

Faire confiance à ce certificat ? [non] :  oui

```
