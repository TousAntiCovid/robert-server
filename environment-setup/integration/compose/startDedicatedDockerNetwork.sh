#! /bin/bash

export $(grep TAC_DOCKER_COMPOSE_NETWORK .env.local)

docker network create $TAC_DOCKER_COMPOSE_NETWORK

docker network ls

