#! /bin/bash


#Down 

docker-compose -f docker-compose-orange-captcha.yml down 
docker-compose -f docker-compose-proxy.yaml down
docker-compose -f docker-compose-robert-crypto.yml down
docker-compose -f docker-compose-robert-push.yml down
docker-compose -f docker-compose-robert-submission.yml down
docker-compose -f docker-compose-tacw.yml down
docker-compose -f docker-compose-robert-server.yaml down


