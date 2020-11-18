# mvn package -Dmaven.test.skip=true
# docker build -f src/main/docker/Dockerfile -t tac/robert-warning-ws-rest .

FROM store/oracle/serverjre:8
ADD ./target/tac-warning-ws-rest-0.0.1-SNAPSHOT.jar /app.jar
ADD ./src/main/resources/application.properties /work/config/application.properties
CMD ["java","-jar", "/app.jar", "--spring.config.location=file:/work/config/application.properties"]
