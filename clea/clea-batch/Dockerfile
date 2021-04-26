FROM centos:7 AS installer


# Install all the locales
RUN sed -i 's/^override_install_langs=.*$/override_install_langs=fr_FR/g' /etc/yum.conf \
    && yum -y update \
    && yum -y install java-11-openjdk \
    && yum -y install epel-release \
    && yum -y install python3 python3-pip \
    && yum -y install nc \
    && useradd -g root javaapp

ENV LC_ALL fr_FR.utf8
ENV LANG fr_FR.utf8
ENV PATH $PATH:~/.local/bin

WORKDIR /home/javaapp

USER javaapp
# Integration WebHook
RUN pip3 install --user s3cmd \ 
 && pip3 install --user flask flask_autoindex

FROM installer
COPY ./target/clea*-exec.jar ./clea-batch.jar
COPY ./src/main/scripts/* ./
COPY ./src/main/docker/* ./http/

USER root
RUN chmod +x *.sh

USER javaapp

ENTRYPOINT [ "./http/web.sh" ]
