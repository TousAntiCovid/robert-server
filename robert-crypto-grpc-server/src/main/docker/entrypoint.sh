#!/bin/bash

function generate_aes_key() {
  local size=$1
  local alias=$2
  echo "Generating $alias"
  keytool -genseckey -alias "$alias" -keyalg AES -keysize "$size" -keystore keystore.p12 -storepass 1234 -storetype PKCS12
}

generate_aes_key 256 federation-key
generate_aes_key 256 key-encryption-key
for i in {-15..14} ; do
  generate_aes_key 192 "server-key-$(date --date="$i days" +%Y%m%d)"
done

wait

exec "$@"
