FROM eclipse-temurin:11-jre-alpine
COPY --from=trajano/alpine-libfaketime  /faketime.so /lib/faketime.so
ENV LD_PRELOAD=/lib/faketime.so

RUN apk add --no-cache bash

COPY ./src/main/docker/keystore.p12 /
COPY ./src/main/docker/entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-jar", "/app.jar"]

COPY ./target/robert-crypto-grpc-server-*.jar app.jar
