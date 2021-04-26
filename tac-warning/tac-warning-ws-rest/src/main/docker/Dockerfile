# mvn package -Dmaven.test.skip=true
# docker build -f src/main/docker/Dockerfile -t registry.gitlab.inria.fr/stopcovid19/backend-server/tac-warning-ws-rest:otac .

FROM registry.gitlab.inria.fr/stemcovid19/tac-server/docker-image/oracle-serverjre:1.8.0_241-b07
ADD ./target/tac-warning-ws-rest-2.3.0-SNAPSHOT-exec.jar /app.jar
#ADD ./src/main/resources/application.properties /work/config/application.properties
ENV ROBERT_JWT_PUBLIC_KEY changeit

CMD ["java","-jar", "/app.jar", "--spring.config.location=file:/work/config/application.properties", "-Dspring-boot.run.arguments=--robert.jwt.publickey=${ROBERT_JWT_PUBLIC_KEY}"]
