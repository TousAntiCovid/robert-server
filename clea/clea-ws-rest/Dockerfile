FROM azul/zulu-openjdk-alpine:11

RUN adduser -D javaapp

COPY ./target/*-exec.jar /home/javaapp/clea-ws-rest.jar

WORKDIR /home/javaapp

ENTRYPOINT ["java","-jar","clea-ws-rest.jar"]
EXPOSE 8080
