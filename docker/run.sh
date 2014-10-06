#! /bin/sh
set -ex
java -Dlog4j.configuration=file:log4j.properties $* -jar /opt/http2kafka.jar
