# mvn package -Dmaven.test.skip=true
# Warning hsm tool must be ./hsm-tools do not forget to call
# git clone https://gitlab.inria.fr/stemcovid19/wp3-robert-server/hsm-tools
# and then rm -rf hsm-tools
# docker build -f src/main/docker/Dockerfile -t tac/robert-crypto-grpc-server .

FROM registry.gitlab.inria.fr/stemcovid19/tac-server/docker-image/tac-oraclejava8-jdk:latest
RUN apt-get update
RUN apt-get -y install git build-essential libtool botan libssl-dev libbotan-2-dev python3 python3-pip opensc
# some cleanup
RUN rm -rf /var/lib/apt/lists/*
# install SoftHSMv2
RUN git clone https://github.com/opendnssec/SoftHSMv2
WORKDIR /SoftHSMv2
RUN ./autogen.sh
RUN ./configure
RUN make
RUN make install
WORKDIR /

# install hsm-tools (must be checked out prior to building this image)
ADD ./hsm-tools /hsm-tools
# RUN git clone https://gitlab.inria.fr/stemcovid19/wp3-robert-server/hsm-tools
WORKDIR /hsm-tools/key-management/init-from-scratch
# verify that python3 is installed
RUN pip3 install python-pkcs11
# check that JAVA_HOME and PATH points to jdk oracle
RUN ./reinstall_HSM_keys.sh
RUN ./install_register_keys.sh 1234

WORKDIR /
RUN apt-get update
RUN apt-get install wget
RUN wget https://golang.org/dl/go1.15.5.linux-amd64.tar.gz
RUN tar -xzf go1.15.5.linux-amd64.tar.gz
RUN rm go1.15.5.linux-amd64.tar.gz
RUN /go/bin/go get github.com/fullstorydev/grpcurl
WORKDIR /root/go/src/github.com/fullstorydev/grpcurl
RUN PATH=/go/bin/:$PATH make install


# install grpc-crypto app
WORKDIR /
ADD ./target/robert-crypto-grpc-server-1.9.1.jar /app.jar
ADD ./src/main/resources/application.properties /work/config/application.properties
CMD ["java","-jar", "/app.jar", "--spring.config.location=file:/work/config/application.properties"]
