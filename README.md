# Installation

```console
docker build -t you/http2kafka .
```

# Usage

Default properties shown:

```console
docker run -it --rm -p 8004:80 codeclimate/http2kafka ./run.sh \
  -Dhttp2kafka.host=0.0.0.0 \
  -Dhttp2kafka.port=80 \
  -Dmetadata.broker.list=localhost:9092
```

Post to Kafka:

```console
curl -XPOST 'http://localhost:8004/' -H Topic:topic -d 'test data'
```

Post to Kafka with a specific message key:

```console
curl -XPOST 'http://localhost:8004/' -H Topic:topic -H Key:key -d 'test data'
```
