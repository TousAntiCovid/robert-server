#! /bin/bash

if [ ! -f ".env" ]; then
    echo "ERROR missing .env file"
    exit 1
fi 

export $(grep TAC_DOCKER_COMPOSE_NETWORK .env)

docker network rm $TAC_DOCKER_COMPOSE_NETWORK

docker network ls

