FROM adoptopenjdk:11-jre-hotspot

RUN apt-get update \
    && apt-get install -y libfaketime \
    && rm -rf /var/lib/apt/lists/*

COPY ./src/main/docker/entrypoint.sh /entrypoint.sh
COPY ./src/main/docker/keystore.p12 /keystore.p12
COPY ./target/robert-crypto-grpc-server-*.jar app.jar

ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/faketime/libfaketime.so.1

ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-jar", "/app.jar"]
