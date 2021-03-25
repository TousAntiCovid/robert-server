FROM azul/zulu-openjdk-alpine:11
EXPOSE 8080
ADD ./target/*-exec.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
