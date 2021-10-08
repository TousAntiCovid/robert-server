FROM adoptopenjdk:11-jre-hotspot
ADD ./target/robert-server-ws-rest-*.jar /app.jar
CMD ["java","-jar", "/app.jar", "-Dspring.profiles.active=docker"]
