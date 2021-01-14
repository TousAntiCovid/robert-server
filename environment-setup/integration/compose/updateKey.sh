#! /bin/bash
#docker exec -w /hsm-tools/key-management/init-from-scratch/  -t -i compose_robert-crypto-grpc-server_1 /hsm-tools/key-management/init-from-scratch/reinstall_HSM_keys.sh
#docker stop compose_robert-crypto-grpc-server_1
#docker start compose_robert-crypto-grpc-server_1
docker exec -t -i robert-crypto-grpc-server python3 /hsm-tools/key-management/add-keys/add_ks_keys_to_hsm.py  -p 1234 -n 100
docker exec -t -i -w /hsm-tools/crypto-server-reload-hsm -e PIN=1234 -eHSM_JAVA_CONFIG=/hsm-tools/key-management/init-from-scratch/softhsm2.cfg -ePATH=/root/go/bin/ robert-crypto-grpc-server  /hsm-tools/crypto-server-reload-hsm/hsm_reload.sh
docker exec -t -i -w /hsm-tools/crypto-server-reload-hsm -e PIN=1234 -eHSM_JAVA_CONFIG=/hsm-tools/key-management/init-from-scratch/softhsm2.cfg -ePATH=/root/go/bin/ robert-crypto-grpc-server  /hsm-tools/crypto-server-reload-hsm/hsm_check_cache.sh
docker stop compose_proxy_1
docker start compose_proxy_1