package nc.isi.http2kafka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.apache.log4j.Logger;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

/**
 * The main server class.
 * 
 * @author Mikaël Cluseau - ISI.NC
 *
 */
public class Server implements Container {

	private static final Logger LOG = Logger.getLogger(Server.class);

	private static Connection connection;

	public static void main(String[] args) throws IOException {
		Properties properties = System.getProperties();

		String bindHostname = properties.getProperty("http2kafka.host");
		int serverPort = Integer.parseInt(properties.getProperty(
				"http2kafka.port", "80"));

		Container container = new Server(filter(properties));
		org.simpleframework.transport.Server server = new ContainerServer(
				container);

		connection = new SocketConnection(server);
		SocketAddress address;
		if (bindHostname != null) {
			LOG.info("Binding to " + bindHostname + ":" + serverPort);
			address = new InetSocketAddress(bindHostname, serverPort);
		} else {
			LOG.info("Binding to *:" + serverPort);
			address = new InetSocketAddress(serverPort);
		}
		connection.connect(address);
	}

	/**
	 * Removes well-known input properties.
	 * 
	 * @param properties
	 *            Input properties.
	 * @return Output properties (filtered view of input).
	 */
	private static Properties filter(Properties properties) {
		Properties output = new Properties();
		for (Object o : properties.keySet()) {
			String property = (String) o;
			if (property.equals("line.separator") //
					|| property.equals("path.separator") //
					|| property.startsWith("java.") //
					|| property.startsWith("user.") //
					|| property.startsWith("sun.") //
					|| property.startsWith("os.") //
					|| property.startsWith("file.") //
					|| property.startsWith("awt.") //
					|| property.startsWith("http2kafka.") //
			) {
				continue;
			}
			output.setProperty(property, properties.getProperty(property));
		}
		return output;
	}

	public static void close() {
		LOG.info("Server shutdown.");
		try {
			connection.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private final KafkaProducer<String, byte[]> producer;

	public Server(Properties producerProperties) {
		String servers = producerProperties.getProperty("metadata.broker.list", "localhost:9092");

		Map <String, Object> hm = new HashMap<String, Object>();
		hm.put("bootstrap.servers", servers);
		hm.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		hm.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

		producer = new KafkaProducer<String, byte[]>(hm);
	}

	public void handle(Request req, Response resp) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Request: " + req.getMethod() + " " + req.getPath());
			LOG.trace(req);
		}
		try {
			checkPath(req);
			ensurePost(req);

			String topic = extractTopic(req);
			String key = extractKey(req);

			sendBody(req, topic, key);

			textResponse(resp, "OK");

		} catch (HttpError error) {
			textResponse(resp, error);

		} catch (Exception e) {
			LOG.fatal("Error while processing request", e);
			textResponse(resp, HttpError.internalServerError(e));
		}
	}

	/**
	 * Check the path of the request. We only allow server's root.
	 * 
	 * @param req
	 *            The client's request.
	 * @throws HttpError
	 *             If the path is invalid.
	 */
	private void checkPath(Request req) throws HttpError {
		String path = req.getPath().getPath();
		if (!(path.isEmpty() || "/".equals(path))) {
			throw HttpError.notFound();
		}
	}

	/**
	 * Ensures the request is a <code>POST</code>.
	 * 
	 * @param req
	 *            The client's request.
	 * @throws HttpError
	 *             If the request is not a POST.
	 */
	private void ensurePost(Request req) throws HttpError {
		if (!"POST".equals(req.getMethod())) {
			throw HttpError.badRequest("POST only");
		}
	}

	/**
	 * Extract the topic from the request.
	 * <p>
	 * The topic is expected to be exactly one <code>Topic</code> header.
	 * 
	 * @param req
	 *            The request.
	 * @return The topic found in the request.
	 * @throws HttpError
	 *             If the request doesn't specify exactly one topic.
	 */
	private String extractTopic(Request req) throws HttpError {
		List<String> topicHeaders = req.getValues("Topic");
		if (topicHeaders.size() != 1) {
			throw HttpError.badRequest("Topic header is missing.");
		}
		String topic = topicHeaders.get(0);
		return topic;
	}

	/**
	 * Extract the key from the request.
	 * <p>
	 * The key is expected to be exactly one <code>Key</code> header.
	 *
	 * @param req
	 *            The request.
	 * @return The key found in the request, or null
	 */
	private String extractKey(Request req) {
		List<String> keyHeaders = req.getValues("Key");
		if (keyHeaders.size() != 1) {
			return null;
		}
		String key = keyHeaders.get(0);
		return key;
	}

	/**
	 * Send the response body to Kafka.
	 * 
	 * @param req
	 *            The client's request.
	 * @param topic
	 *            The target Kafka topic.
	 * @param key
	 *            The Kafka message key.
	 * @throws HttpError
	 *             if an {@link IOException} happens while reading the response.
	 */
	private void sendBody(Request req, String topic, String key) throws HttpError {
		try {
			byte[] message = readReqBody(req);

			if (key != null) {
				producer.send(new ProducerRecord(topic, key, message));
			} else {
				producer.send(new ProducerRecord(topic, message));
			}
		} catch (IOException e) {
			throw HttpError.internalServerError(e);
		}
	}

	private byte[] readReqBody(Request req) throws IOException {
		ByteArrayOutputStream reqBody = new ByteArrayOutputStream();
		InputStream reqInput = req.getInputStream();
		byte[] buf = new byte[4096];
		int readBytes;
		while ((readBytes = reqInput.read(buf)) >= 0) {
			reqBody.write(buf, 0, readBytes);
		}
		reqInput.close();
		return reqBody.toByteArray();
	}

	/**
	 * Sends a simple <code>text/plain</code> response to the client.
	 * 
	 * @param resp
	 *            The response object.
	 * @param error
	 *            The error to render.
	 */
	private void textResponse(Response resp, HttpError error) {
		textResponse(resp, error.getResponseCode(), error.getMessage());
	}

	/**
	 * Sends a simple <code>text/plain</code> response to the client.
	 * 
	 * @param resp
	 *            The response object.
	 * @param responseText
	 *            The response text to send.
	 */
	private void textResponse(Response resp, String responseText) {
		textResponse(resp, 200, responseText);
	}

	/**
	 * Sends a simple <code>text/plain</code> response to the client.
	 * 
	 * @param resp
	 *            The response object.
	 * @param code
	 *            The HTTP response code.
	 * @param responseText
	 *            The response text to send.
	 */
	private void textResponse(Response resp, int code, String responseText) {
		try {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Response: " + code + ": " + responseText);
			}
			resp.setCode(code);
			resp.setContentType("text/plain");
			resp.setContentLength(responseText.length());
			PrintStream stream = resp.getPrintStream();
			stream.print(responseText);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
