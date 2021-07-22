# mvn package -Dmaven.test.skip=true && docker build -f src/main/docker/Dockerfile -t tac/robert-server-ws-rest .
FROM adoptopenjdk:11-jre-hotspot
ADD ./target/robert-server-ws-rest-*.jar /app.jar
ENV ROBERT_JWT_PRIVATE_KEY changeit
ENV ROBERT_JWT_TOKEN_DECLARE_PRIVATE_KEY changeit
ENV ROBERT_JWT_TOKEN_DECLARE_PUBLIC_KID changeit
ENV ROBERT_JWT_TOKEN_ANALYTICS_PRIVATE_KEY changeit
COPY ./src/main/docker/debug_wait.sh /debug_wait.sh
COPY ./src/main/docker/entrypoint.sh /entrypoint.sh

CMD ["java","-jar", "/app.jar", "-Dspring.profiles.active=dev", "-Dspring-boot.run.arguments=--robert.jwt.privatekey=${ROBERT_JWT_PRIVATE_KEY},--robert.jwt.declare.public-kid=${ROBERT_JWT_TOKEN_DECLARE_PUBLIC_KID},--robert.jwt.declare.private-key=${ROBERT_JWT_TOKEN_DECLARE_PRIVATE_KEY},--robert.jwt.analytics.token.private-key=${ROBERT_JWT_TOKEN_ANALYTICS_PRIVATE_KEY}"]

# Debug keep container alive
#RUN chmod +x /entrypoint.sh
#ENTRYPOINT ["/entrypoint.sh"]
