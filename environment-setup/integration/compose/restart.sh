#! /bin/bash

if [ ! -f ".env" ]; then
    echo "ERROR missing .env file"
    exit 1
fi 

# Pull
docker-compose -f docker-compose-orange-captcha.yml  pull
docker-compose -f docker-compose-proxy.yaml  pull
docker-compose -f docker-compose-robert-crypto.yml  pull
docker-compose -f docker-compose-robert-push.yml  pull
docker-compose -f docker-compose-robert-submission.yml  pull
docker-compose -f docker-compose-tacw.yml  pull
docker-compose -f docker-compose-robert-server.yaml  pull

#Down 

docker-compose -f docker-compose-orange-captcha.yml  down
docker-compose -f docker-compose-proxy.yaml  down
docker-compose -f docker-compose-robert-crypto.yml down
docker-compose -f docker-compose-robert-push.yml down
docker-compose -f docker-compose-robert-submission.yml down
docker-compose -f docker-compose-tacw.yml down
docker-compose -f docker-compose-robert-server.yaml down

#start
docker-compose -f docker-compose-orange-captcha.yml    up -d
docker-compose -f docker-compose-robert-crypto.yml     up -d
docker-compose -f docker-compose-robert-push.yml       up -d
docker-compose -f docker-compose-robert-submission.yml up -d
docker-compose -f docker-compose-tacw.yml              up -d
docker-compose -f docker-compose-robert-server.yaml    up -d
docker-compose -f docker-compose-proxy.yaml            up -d

#update keys on crypto

sleep 10
./updateKey.sh