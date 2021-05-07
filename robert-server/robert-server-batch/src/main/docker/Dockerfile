# in robert-crypto-grpc-server folder mvn clean package -dmaven.test.skip=true
# docker build -f ./Dockerfile-openjdk8 -t tac/robert-crypto-server .

FROM adoptopenjdk:11-jre-hotspot

WORKDIR /opt/robert-server-batch/

COPY src/main/docker/logback.xml logback.xml
COPY target/robert-server-batch*.jar app.jar
COPY src/main/docker/entrypoint.sh /entrypoint.sh
#COPY src/main/docker/run.sh /run.sh

RUN apt-get update && apt-get -y install dos2unix \
   && echo 'some cleanup' && rm -rf /var/lib/apt/lists/*

RUN dos2unix /entrypoint.sh && chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
