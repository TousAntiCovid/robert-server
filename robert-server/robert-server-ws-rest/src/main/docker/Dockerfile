# mvn package -Dmaven.test.skip=true && docker build -f src/main/docker/Dockerfile -t tac/robert-server-ws-rest .
FROM registry.gitlab.inria.fr/stemcovid19/tac-server/docker-image/oracle-serverjre:1.8.0_241-b07
ADD ./target/robert-server-ws-rest-1.10.0-SNAPSHOT.jar /app.jar
ADD ./src/main/resources/application.properties /work/config/application.properties
ENV ROBERT_JWT_PRIVATE_KEY changeit
ENV ROBERT_JWT_TOKEN_DECLARE_PRIVATE_KEY changeit
ENV ROBERT_JWT_TOKEN_DECLARE_PUBLIC_KID changeit
CMD ["java","-jar", "/app.jar", "--spring.config.location=file:/work/config/application.properties", "-Dspring-boot.run.arguments=--robert.jwt.privatekey=${ROBERT_JWT_PRIVATE_KEY},--robert.jwt.declare.public-kid=${ROBERT_JWT_TOKEN_DECLARE_PUBLIC_KID},--robert.jwt.declare.private-key=${ROBERT_JWT_TOKEN_DECLARE_PRIVATE_KEY}"]
