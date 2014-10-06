from dockerfile/java:latest
maintainer Mikaël Cluseau

add http2kafka.jar /opt/

add run.sh /opt/
add log4j.properties /opt/

workdir /opt
cmd ./run.sh
expose 80
