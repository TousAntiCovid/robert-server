# keystore

This keystore has been initialized with a private key and a certificate that will be used on dev environment and continuous integration.

## procedure

1. generate a private key for a curve (register key used by the application)

```shell
openssl ecparam -name prime256v1 -genkey -noout -out private-key.pem
openssl pkcs8 -topk8 -inform pem -in private-key.pem -outform pem -nocrypt -out register-key-private-key.pem
```

2. generate the certificate

```shell
openssl req -x509 -key register-key-private-key.pem -days 3600 -out register-key-certificate.pem
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:.
State or Province Name (full name) [Some-State]:.
Locality Name (eg, city) []:.
Organization Name (eg, company) [Internet Widgits Pty Ltd]:.
Organizational Unit Name (eg, section) []:.
Common Name (e.g. server FQDN or YOUR name) []:DN: CN=StopCovid
Email Address []:.
```

3. generate the PKCS12 keystore

```shell
openssl pkcs12 -export -in register-key-certificate.pem -inkey register-key-private-key.pem -name "register-key" -out keystore.p12 -password pass:1234
```

4. to get the public key : it will be shared to the mobile application (set to "emulator.robert-crypto-public-key" property of the robert server mobile app emulator)

```shell
openssl x509 -pubkey -noout -in register-key-certificate.pem  > register-key-public-key.pem
```

# 5. to check the content of the keystore

```shell
keytool -list -keystore keystore.p12 -v -storepass 1234
```
