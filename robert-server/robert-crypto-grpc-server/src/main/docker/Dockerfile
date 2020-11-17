# mvn package -Dmaven.test.skip=true
# docker build -f src/main/docker/Dockerfile -t tac/robert-crypto-grpc-server .

FROM store/oracle/serverjre:8
ADD ./target/robert-crypto-grpc-server-1.7.0-SNAPSHOT.jar /app.jar
ADD ./src/main/resources/application.properties /work/config/application.properties
CMD ["java","-jar", "/app.jar", "--spring.config.location=file:/work/config/application.properties"]
