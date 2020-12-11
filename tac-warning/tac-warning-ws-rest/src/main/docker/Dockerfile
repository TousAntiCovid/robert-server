# mvn package -Dmaven.test.skip=true
# docker build -f src/main/docker/Dockerfile -t registry.gitlab.inria.fr/stemcovid19/tac-server/backend-server/tac-warning-ws-rest:otac .

FROM store/oracle/serverjre:8
ADD ./target/tac-warning-ws-rest-1.1.0-SNAPSHOT-exec.jar /app.jar
#ADD ./src/main/resources/application.properties /work/config/application.properties
ENV ROBERT_JWT_PUBLIC_KEY changeit

CMD ["java","-jar", "/app.jar", "--spring.config.location=file:/work/config/application.properties", "-Dspring-boot.run.arguments=--robert.jwt.publickey=${ROBERT_JWT_PUBLIC_KEY}"]
