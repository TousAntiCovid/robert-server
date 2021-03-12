#!/bin/sh

echo "Launch appli"
cd /opt/robert-server-batch
java $JAVA_OPTS -jar -Dspring.profiles.active=dev app.jar --logging.config=/opt/robert-server-batch/logback.xml

#. /run.sh