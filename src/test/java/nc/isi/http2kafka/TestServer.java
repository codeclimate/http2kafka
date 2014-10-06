package nc.isi.http2kafka;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

/**
 * This test assumes you have at least one Kafka broker on localhost:9092 and a
 * working topic named "topic".
 * 
 * @author Mikaël Cluseau - ISI.NC
 *
 */
public class TestServer extends TestCase {

	@Override
	protected void setUp() throws Exception {
		startServer();
	}

	@Override
	protected void tearDown() throws Exception {
		Server.close();
	}

	public void testGoodRequest() throws Exception {
		HttpClient client = new HttpClient();
		PostMethod post = postMessage(client, "test data");

		assertEquals("OK", post.getResponseBodyAsString());
	}

	public void testPerf() throws Exception {
		Thread[] threads = new Thread[10];
		final int messagesPerThread = 1_000;

		for (int t = 0; t < threads.length; t++) {
			threads[t] = new Thread(new Runnable() {
				public void run() {
					HttpClient client = new HttpClient();
					for (int i = 0; i < messagesPerThread; i++) {
						PostMethod post = postMessage(client, "test data " + i);
						try {
							assertEquals("OK", post.getResponseBodyAsString());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			});
		}

		long startTime = System.currentTimeMillis();
		for (int t = 0; t < threads.length; t++) {
			threads[t].start();
		}
		for (int t = 0; t < threads.length; t++) {
			threads[t].join();
		}
		long duration = (System.currentTimeMillis() - startTime);
		int messageCount = threads.length * messagesPerThread;

		System.out.println(messageCount + " messages sent in " + duration
				+ "ms (" + (messageCount * 1000 / duration) + " req/s)");
	}

	private PostMethod postMessage(HttpClient client, String message) {
		try {
			PostMethod post = new PostMethod("http://localhost:8004");
			post.addRequestHeader("Topic", "topic");
			post.setRequestEntity(new StringRequestEntity(message,
					"text/plain", "UTF-8"));
			client.executeMethod(post);
			return post;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void startServer() throws Exception {
		System.setProperty("http2kafka.port", "8004");
		System.setProperty("metadata.broker.list", "localhost:9092");
		Server.main(new String[] {});
	}

}
