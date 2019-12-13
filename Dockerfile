FROM tomcat:9.0-jre8-alpine
EXPOSE 32000
VOLUME /artifacts
WORKDIR /data

COPY entrypoint.sh /
ENTRYPOINT /entrypoint.sh

COPY target/hw3-1.0-SNAPSHOT.jar /app/hw3.jar
