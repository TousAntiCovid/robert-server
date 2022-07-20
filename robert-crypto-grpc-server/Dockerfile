FROM eclipse-temurin:11-jre-focal

RUN apt-get update && apt-get install -y \
    libfaketime \
    wget \
 && rm -rf /var/lib/apt/lists

ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/faketime/libfaketime.so.1

COPY ./src/main/docker/keystore.p12 /
COPY ./src/main/docker/entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-jar", "/app.jar"]

COPY ./target/robert-crypto-grpc-server-*.jar app.jar