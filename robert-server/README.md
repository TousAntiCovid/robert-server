# Introduction
Ce projet gitlab.inria.fr est un des composants de la solution plus globale [StopCovid](https://gitlab.inria.fr/stopcovid19/accueil/-/blob/master/README.md).

Ce composant propose les services suivants :
* Service Register : permet l’inscription à la solution StopCovid et la récupération d’identifiants anonymisés
* Service Report : permet la remontée de contacts suite à une déclaration volontaire de test positif au Covid-19 et l'analyse de leurs risques d'exposition
* Service Status : permet la vérification par une app donnée de son risque d’exposition au Covid-19 
* Service Unregister : permet la désinscription à la solution StopCovid

Le composant implémente également une fédération des initiatives nationales utilisant le même protocole afin de protéger les utilisateurs se déplaçant à l'étranger.

# dev environment

## ROBERT SERVER

To run, robert-crypto-grpc-server needs SoftHsmV2.

So this application is running in a dedicated Docker container (See [DockerFile-openjdk8](./robert-crypto-grpc-server/Dockerfile-openjdk8))

If the source of this application is updated, you have to rebuild your docker environment :

    cd /robert-server/robert-crypto-grpc-server
    mvn clean install -DskipTests
    
    cd /robert-server
    docker-compose -f ./docker-compose-openjdk8.yaml build

After that you can run you docker environment :

    docker-compose -f ./docker-compose-openjdk8.yaml up -d

It is possible to run the `robert-server-ws-rest` application on your dev laptop using `dev` profile

To stop you docker environment :

    docker-compose -f ./docker-compose-openjdk8.yaml down -v

### Orange captcha

This application isn't under the responsability of the TAC team.

So, it is mocked using [MockServer](https://www.mock-server.com/) standalone application.

The Mockserver loads default [expectations file](../environment-setup/dev/compose/captcha/mock-server-expectation.json) during its start-up.  
