FROM java:openjdk-7-jdk
MAINTAINER Pat Brisbin <pat@codeclimate.com>

RUN \
  apt-get update && \
  apt-get install -y maven && \
  rm -rf /var/lib/apt/lists/*

RUN \
  wget http://downloads.sourceforge.net/project/simpleweb/simpleweb/5.1.6/simple-5.1.6.tar.gz && \
  tar axf simple-5.1.6.tar.gz && \
  mvn install:install-file \
    -DgroupId=org.simpleframework \
    -DartifactId=simpleweb \
    -Dpackaging=jar \
    -Dversion=5.1.6 \
    -Dfile=simple-5.1.6/jar/simple-5.1.6.jar \
    -DgeneratePom=true

RUN mkdir -p /app
ADD pom.xml /app/pom.xml
ADD src /app/src
WORKDIR /app
RUN mvn package -DskipTests

RUN mkdir -p /opt
RUN cp target/http2kafka-*-jar-with-dependencies.jar /opt/http2kafka.jar

ADD run.sh /opt/
ADD log4j.properties /opt/

WORKDIR /opt
CMD ./run.sh
EXPOSE 80
