#! /bin/bash

if [ ! -f ".env" ]; then
    echo "ERROR missing .env file"
    exit 1
fi 

# Pull
docker-compose -f docker-compose-robert-server-openjdk11.yaml pull

#Down 

docker-compose -f docker-compose-robert-server-openjdk11.yaml down

#start
docker-compose -f docker-compose-robert-server-openjdk11.yaml up -d

#update keys on crypto

sleep 10
./updateKey.sh