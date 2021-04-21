# Start the backend via docker-compose

Docker compose for local depployment of the backend server (development mode or basic CI)

Instructions to start the backend server locally.

## Create a JWT key pair:

Use script in https://gitlab.inria.fr/stopcovid19/backend-server/-/tree/develop/utils/jwt_keygen

## Create a local docker compose env file (.env.local)

```
cp .env .env.local
```

adapt it according to your needs:
- change ROBERT_JWT_PRIVATE_KEY and ROBERT_JWT_PUBLIC_KEY in order to point to the your JWT keys
- optionnaly, change the DOCKER_TAG key is used to indicate which docker will be retrieved from `registry.gitlab.inria.fr`, you can indicate "latest" or 
a specific branch that was built by the CI (cf. https://gitlab.inria.fr/stopcovid19/backend-server/container_registry )
By default the tag pattern used by the CI is `ci-transient-<BRANCHNAME>`
 (note that these iamages may be reclaimed by the Cleanup policy in https://gitlab.inria.fr/stopcovid19/backend-server/-/settings/ci_cd)
 
 
## Start the backend

```
docker login registry.gitlab.inria.fr
./restart.sh
```

use your gitlab user and password for docker login (gitlab-ci can use  $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD variable). 

if you need to keep the shell you can add the `-d` option 


shutdown is done using:

```
docker-compose down
```


Tips for development:

Do not start a single service (eg. robert-warning-ws-rest) for example in order to start it manually from your IDE:

```
# Change the docker compose file depending of the service you plan to update
docker-compose -f docker-compose-robert-server.yaml --env-file .env.local up --scale robert-warning-ws-rest=0
```

Stop a single service (if you just want to play with a specific spring property for example):

```
docker stop robert-warning-ws-rest
# next docker start to restart it (it just kill the process)
docker start robert-warning-ws-rest

```

Update the images from the registry

```
# Change the docker compose file depending of the service you plan to update
docker-compose -f docker-compose-robert-server.yaml --env-file .env.local pull
```
