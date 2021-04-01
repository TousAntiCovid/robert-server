FROM centos:7 AS installer

# Install all the locales
RUN sed -i 's/^override_install_langs=.*$/override_install_langs=fr_FR/g' /etc/yum.conf \
    && yum -y update \
    && yum -y install java-11-openjdk \
    && yum -y install epel-release \
    && yum -y install python3 python3-pip \
    && pip3 install s3cmd \ 
    && useradd javaapp

WORKDIR /home/javaapp

FROM installer
COPY ./target/clea*-exec.jar ./clea-batch.jar
COPY ./src/main/scripts/* ./
RUN chmod +x *.sh
RUN mkdir /v1 && chown -R javaapp:root /v1


USER javaapp

ENTRYPOINT [ "./clea-batch.sh" ]
