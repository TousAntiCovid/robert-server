FROM adoptopenjdk:11-jre-hotspot
ADD ./target/robert-server-ws-rest-*.jar /app.jar
RUN mkdir - mkdir /var/log/robert-ws-server
CMD ["java","-jar", "/app.jar"]
