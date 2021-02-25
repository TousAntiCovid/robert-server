# in robert-crypto-grpc-server folder mvn clean package -Dmaven.test.skip=true
# docker build -f src/main/docker/Dockerfile -t tac/robert-crypto-grpc-server .

FROM registry.gitlab.inria.fr/stemcovid19/wp3-robert-server/hsm-tools/tac-soft-hsm-v2-openjdk8:latest

WORKDIR /opt/robert-crypto-server/

COPY ./src/main/resources/application.properties /work/config/application.properties
COPY ./src/main/docker/logback.xml /work/config/logback.xml
COPY ./target/robert-crypto-grpc-server-*.jar app.jar
COPY ./src/main/docker/entrypoint.sh /entrypoint.sh

# this is useful to debug
COPY ./src/main/docker/run.sh /run.sh

RUN dos2unix /entrypoint.sh && chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
