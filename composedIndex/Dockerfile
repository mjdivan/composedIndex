# To change this license header, choose License Headers in Project Properties.
# To change this template file, choose Tools | Templates
# and open the template in the editor.
FROM openjdk:8-jdk-alpine

CMD ["/bin/sh"]
ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y \
        maven \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml /tmp

#COPY postInstall /
#RUN /postInstall


RUN cd /tmp && mvn package && rm -rf /tmp/target