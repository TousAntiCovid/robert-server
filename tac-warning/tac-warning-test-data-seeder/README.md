This module provides a simple way to seed TAC Warning database with random visits.

## Generate the app and package it

```bash
mvn package
```

Copy the jar file on the target server

```bash
scp target/tac-warning-test-data-seeder-*-exec.jar  server:/tmp
```

## Specify database properties

If the database server does not have a fixed IP address, you have to find it, by example, running a docker command on the Docker node.

```bash
sudo docker inspect compose_postgreswarning_1 | grep IPAddress
```

Copy the database.properties file from `https://gitlab.inria.fr/stopcovid19/backend-server/-/tree/develop/tac-warning/tac-warning-test-data-seeder/src/main/resources` to the current directory and update the following properties:
  - spring.datasource.url
  - spring.datasource.username
  - spring.datasource.password
It should look like following:

```properties
spring.datasource.url= jdbc:postgresql://172.1.0.10:5432/warning?reWriteBatchedInserts=true
spring.datasource.username=joe
spring.datasource.password=foo
...
```

Then copy the application.properties file (if default values do not fit the need) to the current directory and update the properties.

## Run the database seeding
Prepare the environment

```bash
export JAVA_HOME=/opt/jdk8u275-b01-jre
export PATH=$JAVA_HOME/bin:$PATH
```

Run the database seeding

```bash
java -jar /tmp/tac-warning-test-data-seeder-*-exec.jar --spring.config.location=file:./database.properties,file:./application.properties
```
If you want to use default application properties, replace  `file:./application.properties` by `file:./src/main/resources/application.properties` in the previous command.

## Check that database has the expected data
 
```bash
psql -h 172.1.0.10 -U robert-warning warning
>  select count(*) from exposed_static_visit;
 ```
 
 