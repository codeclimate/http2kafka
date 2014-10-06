
# Usage

Run the jar:

    java -jar http2kafka.jar \
         -Dhttp2kafka.port=8004 \
         -Dmetadata.broker.list=broker1:9092,broker2:9092 \
         nc.isi.http2kafka.Server

Run from maven:

	mvn exec:java -Dexec.mainClass=nc.isi.http2kafka.Server \
	    -Dhttp2kafka.port=8004 \
	    -Dmetadata.broker.list=localhost:9092

Post to Kafka:

	curl -XPOST 'http://localhost:8004/' -H Topic:topic -d 'test data'

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
