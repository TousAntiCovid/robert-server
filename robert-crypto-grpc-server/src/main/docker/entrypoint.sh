#!/usr/bin/env bash

# 👇 this ensures libfaketime is not active while generating keys
export LD_PRELOAD=""
date -I'seconds' -u

function generate_aes_key() {
  local size=$1
  local alias=$2
  echo "Generating $alias"
  keytool -genseckey -alias "$alias" -keyalg AES -keysize "$size" -keystore keystore.p12 -storepass 1234 -storetype PKCS12
}

generate_aes_key 256 federation-key
generate_aes_key 256 key-encryption-key
now=$(date +%s)
for i in {-20..5} ; do
  timestamp=$(( $now + $i * 24 * 60 * 60 ))
  generate_aes_key 192 "server-key-$(date -d "@$timestamp" +%Y%m%d)"
done

exec "$@"
