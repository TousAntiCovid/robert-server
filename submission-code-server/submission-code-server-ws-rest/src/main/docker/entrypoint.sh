#!/bin/sh

echo "Launch appli"
cd /opt/submission-code-server
java $JAVA_OPTS -Dlogging.config=/opt/submission-code-server/logback.xml -jar /opt/submission-code-server/app.jar --spring.config.location=file:/opt/submission-code-server/application.properties

#. /run.sh