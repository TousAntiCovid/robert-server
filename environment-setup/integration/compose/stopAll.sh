#! /bin/bash


#Down 

docker-compose -f docker-compose-orange-captcha.yml -v down 
docker-compose -f docker-compose-proxy.yaml -v down
docker-compose -f docker-compose-robert-crypto.yml -v down
docker-compose -f docker-compose-robert-push.yml -v down
docker-compose -f docker-compose-robert-submission.yml -v down
docker-compose -f docker-compose-tacw.yml -v down
docker-compose -f docker-compose-robert-server.yaml -v down


