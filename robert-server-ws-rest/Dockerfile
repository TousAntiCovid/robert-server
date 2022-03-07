FROM adoptopenjdk:11-jre-hotspot
ADD ./target/robert-server-ws-rest-*.jar /app.jar
RUN apt-get update \
    && apt-get install -y libfaketime
ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/faketime/libfaketime.so.1
CMD ["java", "-jar", "/app.jar"]
