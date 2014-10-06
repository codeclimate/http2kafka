package nc.isi.http2kafka;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * HttpError exception class to send errors to the client.
 * 
 * @author Mikaël Cluseau - ISI.NC
 *
 */
@SuppressWarnings("serial")
public class HttpError extends Exception {

	public static HttpError badRequest(String message) {
		return new HttpError(400, message);
	}

	public static HttpError notFound() {
		return new HttpError(404, "Not Found");
	}

	public static HttpError internalServerError(String message) {
		return new HttpError(500, message);
	}

	public static HttpError internalServerError(Throwable throwable) {
		StringWriter buf = new StringWriter();
		throwable.printStackTrace(new PrintWriter(buf));
		return new HttpError(500, buf.toString());
	}

	private int responseCode;

	public HttpError(int responseCode, String message) {
		super(message);
		this.responseCode = responseCode;
	}

	public int getResponseCode() {
		return responseCode;
	}

}
