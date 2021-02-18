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

If not already done, please log in to the inria gitlab

    docker login registry.gitlab.inria.fr

To run, robert-crypto-grpc-server needs SoftHsmV2.

So this application is running in a dedicated Docker container (See [DockerFile](./robert-crypto-grpc-server/src/main/docker/Dockerfile))

If the source of this application is updated, you have to rebuild your docker environment :

    # from robert-server folder
    mvn clean install -DskipTests 
    docker-compose -f ../environment-setup/dev/compose/docker-compose-robert-server-openjdk8.yaml build

After that you can run you docker environment :

    docker-compose -f ../environment-setup/dev/compose/docker-compose-robert-server-openjdk8.yaml up -d

It is possible to run the `robert-server-ws-rest` application on your dev laptop using `dev` profile

To stop you docker environment :

    docker-compose -f ../environment-setup/dev/compose/docker-compose-robert-server-openjdk8.yaml down -v

### Orange captcha

This application isn't under the responsability of the TAC team.

So, it is mocked using [MockServer](https://www.mock-server.com/) standalone application.

The Mockserver loads default [expectations file](../environment-setup/dev/compose/captcha/mock-server-expectation.json) during its start-up.  

# Prepare a release

This release preparation is based on the make a release section of the [git-flow cheatsheet](https://danielkummer.github.io/git-flow-cheatsheet/).

Nevertheless, operations are manually done because currently develop and master branches are protected. 

For example, in this chapter we'll prepare the release 1.8.0

* From the desired commit on `develop` branch, create a release branch named `release/robert-server-1.8.0`

* Bump the new release version to the pom.xml files

      mvn versions:set -DgenerateBackupPoms=false -DnewVersion=1.8.0

/!\ **warning on the robert-server-data-injector module which is not identified in the parent's one, 
the modification must be manually done** /!\

* set the version to the docker file pointing on Robert server applications to the same value as the pom.xml new version 
  (e.g : robert-crypto-grpc-server and robert-server-ws-rest docker files)
  
* commit your modification on the release branch

      git commit -m"[RELEASE] prepare robert server 1.8.0 release"

* push the release branch to the remote

* verify that the IC succeeds (including docker and system tests)

* add a `robert-server-1.8.0` tag for the version and push it.

* add a release note on gitlab following the markdow pattern :

      **New features & improvements**
      
      - Blablabla : <commit_tag>
      
      
      **Bug fix**
      
      - Blablabla  : <commit_tag>

* merge this release branch to `master` branch using merge request

* set the next development version (increment pom.xml and dockerfile to the next version including `-SNAPSHOT`)

      mvn versions:set -DgenerateBackupPoms=false -DnewVersion=1.9.0-SNAPSHOT

* commit 

      git commit -m"[RELEASE] prepare robert server 1.9.0-SNAPSHOT release"

* push the release branch to the remote

* verify that the IC succeeds (including docker and system tests)

* merge this release branch to `develop` branch using merge request

* remove release branch

