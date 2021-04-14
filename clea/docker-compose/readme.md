
# CLEA main docker-compose projet


this project mainlty contains a script *clea* and a *docker-compose.yml* file that compose a minimal stack for CLEA project:
* clea-ws-rest report API
* kafka and zookeeper
* clea-venue-consumer that evaluate visists
* postgres and pgadmin
* clea-batch that build cluster files and clusterIndex file.

The *clea* script is a think wrapper of *docker-compose* executable: it execute this command:

```bash
docker-compose -p clea -f $pwd/docker-compose.yml
```

the *-p* parameter change the stack name wich is, by default the folder name (docker-compose)
the *-f* parameter give the stack file to load. this script compute the absolute path of the file

The *clea* can be place in PATH, can be use in an alias to be launch from folder.

All commands available with docker-compose are available with clea command:

**Manage stack**:

```bash
clea up
clea down
```

**Build images**

clea can be used to build images of ws-rest, venue-consumer and the batch

```bash
clea build # build all three images
clea build clea-batch # (re-) build clea-batch image
```

**Manage services**

A docker-compose files declare services like *postgres* or *clea-ws-rest*. 

*clea* like *docker-compose* command can start or stop a service

```bash
clea up -d postgres pgadmin # launch both posgres and pgadmin in the background
clea stop pgadmin # stop a container
clea rm -f pgadmin # remove container context and attached volumes
```

**restart command**

When we develop a service, the need to restart a service with the latest version is important. *clea* defines a custom command *restart* that stop the container, remove it's state and restart the service.

Here is a typical usage of this command:

```bash
cd clea-ws-rest
mvn package # compile the java project
clea build clea-ws-rest # (re-)create the docker image
clea restart clea-ws-rest # stop previous version, restart a new own
```

this command is also usefull for database. To clean a database and restart with a new empty database, it is not enough to stop the container

```bash
clea restart postgres # restart the instance with no persisted volume
```

this command is equivalent to 
```bash
docker-compose -p clea stop postgres
docker-compose -p clea rm -f postgres
docker-compose -p clea up -d postgres
```

## Extended stacks

This project contains a main *docker-compose.yml* files but also some *overrides* stacks.

Thoses files add new services and can change configuration of pre-define services.

The *docker-compose* command accept many *-f* parameters. this command *merge* thoses files to create a uniq stack file then apply this configuration.

to simplify the use of multiples *-f* parameters with long filenames, clea define a *-o* (for override) parameter that search files named with this pattern: *docker-compose_override_[name].yml*

Here are some commands using the stack with a minio service (bucker server):

```bash
clea -o minio up -d
clea -o minio build clea-batch
clea -o minio restart clea-batch
clea -o minio exec clea-batch /home/javaapp/clea-batch.sh
curl -v http://localhost:9100/clea-batch/v1/clusterIndex.json
clea -o minio restart minio # reset bucket content
clea -o minio down
```

working with overrides is easier by defining alias 
```bash
alias clea-mc="clea -o minio"
clea-mc up -d
clea-mc build clea-batch
clea-mc restart clea-batch
clea-mc exec clea-batch /home/javaapp/clea-batch.sh
curl -v http://localhost:9100/clea-batch/v1/clusterIndex.json
clea-mc restart minio # reset bucket content
clea-mc down
```

## Kafka with SSL

The *docker-compose_override_ssl.yml* change the configuration of kafka, clea-ws-rest, clea-venue-consumer and kafdrop to use TLS communications.

This overlay don't add new services but change the configuration (volumes and environment variables) of existing services.

before using this extension, you need to go to the kafka folder and generate certificats with the script "make_certs"

```bash
cd kafka
./make_certs.sh kafka
```

the stack usage is not modified, except that all communications with kafka are secured

```bash
alias clea-ks="clea -o ssl"
clea-ks up -d
clea-ks logs clea-venue-consumer
clea-ks down
```

## Kong

Kong is the api-gateway used to expose and monitor services (clea-ws-rest).

The *docker-compose_override_front.yml* file contains new services:
* nginx to expose swagger-ui/, api
* kong, the api-gateway itself
* konga to monitor usages.

This stack launch a **no-database** kong instance. the services definitions are defined in kong folder as yaml description and launch when the kong instance started.

This usage is enough to develop new services definitions.

In this mode, Konga can't be used to manage and alter kong configuration but can extract statistiques and usage from Kong.

Konga can be used to monitor the service.


## All-In-One

Stack can be combined to have a full stack

```bash
alias clea-full="clea -o ssl -o minio -o front"
clea-full up -d
curl -v -X POST http://localhost/api/clea/v1/wreport -d "{}"
clea-full logs clea-ws-rest
clea-full down
```





