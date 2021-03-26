FROM azul/zulu-openjdk-alpine:11

RUN adduser -D javaapp

COPY ./target/*-exec.jar /home/javaapp/clea-venue-consumer.jar

WORKDIR /home/javaapp

ENTRYPOINT ["java","-jar","clea-venue-consumer.jar"]
EXPOSE 8080
