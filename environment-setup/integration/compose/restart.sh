#! /bin/bash

# Pull
docker-compose -f docker-compose-orange-captcha.yml --env-file .env.local pull
docker-compose -f docker-compose-proxy.yaml --env-file .env.local pull
docker-compose -f docker-compose-robert-crypto.yml --env-file .env.local pull
docker-compose -f docker-compose-robert-push.yml --env-file .env.local pull
docker-compose -f docker-compose-robert-submission.yml --env-file .env.local pull
docker-compose -f docker-compose-tacw.yml --env-file .env.local pull
docker-compose -f docker-compose-robert-server.yaml --env-file .env.local pull

#Down 

docker-compose -f docker-compose-orange-captcha.yml --env-file .env.local down
docker-compose -f docker-compose-proxy.yaml --env-file .env.local down
docker-compose -f docker-compose-robert-crypto.yml --env-file .env.local down
docker-compose -f docker-compose-robert-push.yml --env-file .env.local down
docker-compose -f docker-compose-robert-submission.yml --env-file .env.local down
docker-compose -f docker-compose-tacw.yml --env-file .env.local down
docker-compose -f docker-compose-robert-server.yaml --env-file .env.local down

#start
docker-compose -f docker-compose-orange-captcha.yml  --env-file .env.local up -d
docker-compose -f docker-compose-robert-crypto.yml  --env-file .env.local up -d
docker-compose -f docker-compose-robert-push.yml  --env-file .env.local up -d
docker-compose -f docker-compose-robert-submission.yml  --env-file .env.local up -d
docker-compose -f docker-compose-tacw.yml  --env-file .env.local up -d
docker-compose -f docker-compose-robert-server.yaml  --env-file .env.local up -d
docker-compose -f docker-compose-proxy.yaml  --env-file .env.local up -d

#update keys on crypto

sleep 10
./updateKey.sh