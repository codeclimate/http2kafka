
# Usage

Run the jar:

    java -Dhttp2kafka.port=8004 \
         -Dmetadata.broker.list=broker1:9092,broker2:9092 \
         -jar http2kafka.jar

Run from maven:

	mvn exec:java -Dexec.mainClass=nc.isi.http2kafka.Server \
	    -Dhttp2kafka.port=8004 \
	    -Dmetadata.broker.list=localhost:9092

Post to Kafka:

	curl -XPOST 'http://localhost:8004/' -H Topic:topic -d 'test data'

## Docker

Run the image from docker hub:

	docker run -d -p 8004:80 mcluseau/http2kafka ./run.sh -Dmetadata.broker.list=172.17.42.1:9092

Build and run locally:

	cd docker
	./build
	docker run -d -p 8004:80 http2kafka ./run.sh -Dmetadata.broker.list=172.17.42.1:9092

# Properties

http2kafka specific properties are prefixed by http2kafka. They are:

* `http2kafka.host`: the hostname to bind to (defaults to all).
* `http2kafka.port`: the port to bind to (defaults to 80).

Not well-known system properties are given to the Kafka producer.

# Dependencies

The maven dependency to org.simpleframework:simpleweb:5.1.6 is hand made. You 
can install it locally with:

	V=5.1.6
	wget http://downloads.sourceforge.net/project/simpleweb/simpleweb/$V/simple-$V.tar.gz
	tar axf simple-$V.tar.gz
	mvn install:install-file \
	    -DgroupId=org.simpleframework \
	    -DartifactId=simpleweb \
	    -Dpackaging=jar \
	    -Dversion=$V \
	    -Dfile=simple-$V/jar/simple-$V.jar \
	    -DgeneratePom=true
