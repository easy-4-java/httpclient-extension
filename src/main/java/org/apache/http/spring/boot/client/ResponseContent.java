package org.apache.http.spring.boot.client;

import java.io.InputStream;
import java.util.Map;

/**
 * Plain-old data holder for an HTTP response, decoded in a form convenient for
 * higher-level Spring-Boot consumers.
 *
 * <p>The class exposes the most useful aspects of a response in a single
 * object: raw bytes, decoded text, character encoding, MIME type, status code
 * and a flattened view of all headers. It deliberately mirrors the typical
 * shape of objects produced by utilities such as Apache HttpClient's
 * {@code ResponseHandler} or Spring's {@code RestTemplate}, while remaining
 * framework-agnostic so it can be reused in non-Spring contexts.</p>
 *
 * <p>All fields are mutable so the object can be populated progressively by
 * response handlers. Threads are not required to synchronise access; each
 * thread is expected to use its own instance.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see org.apache.http.HttpResponse
 */
public class ResponseContent {

	/** The character encoding declared by the response (e.g. {@code "UTF-8"}); may be {@code null}. */
	private String encoding;
	/** The raw response body, still encoded; may be {@code null} if the body was streamed. */
	private byte[] contentBytes;
	/** The HTTP status code; {@code 0} if the response was not received. */
	private int statusCode;
	/** The response body decoded as a string; may be {@code null}. */
	private String contentText;
	/** The parsed content type, stripped of parameters (e.g. {@code "text/html"}); may be {@code null}. */
	private String contentType;
	/** The raw {@code Content-Type} header value, including parameters; may be {@code null}. */
	private String contentTypeString;
	/** An open {@link InputStream} over the body; typically only valid while the connection is open. */
	private InputStream content;
	/** A flattened snapshot of every response header, keyed by header name. */
	private Map<String, String> allHeaders;

	/**
	 * Returns the character encoding associated with the response body.
	 *
	 * @return the declared character encoding, or {@code null} if not set.
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Sets the character encoding that should be used to decode the response body.
	 *
	 * @param encoding the character encoding, typically supplied via the
	 *                 {@code Content-Type} header; may be {@code null}.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Returns the raw response body as a byte array.
	 *
	 * @return the raw bytes, or {@code null} if the body was consumed via {@link #getContent()}.
	 */
	public byte[] getContentBytes() {
		return contentBytes;
	}

	/**
	 * Stores the raw response body for later inspection.
	 *
	 * @param contentBytes the body bytes; may be {@code null}.
	 */
	public void setContentBytes(byte[] contentBytes) {
		this.contentBytes = contentBytes;
	}

	/**
	 * Returns the HTTP status code of the response.
	 *
	 * @return the numeric status code (e.g. {@code 200}, {@code 404}); {@code 0} when unset.
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Stores the HTTP status code of the response.
	 *
	 * @param statusCode the numeric status code.
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Returns the response body decoded as a string using {@link #getEncoding()}.
	 *
	 * @return the decoded text, or {@code null} if not decoded.
	 */
	public String getContentText() {
		return contentText;
	}

	/**
	 * Stores the response body decoded as a string.
	 *
	 * @param contentText the decoded text; may be {@code null}.
	 */
	public void setContentText(String contentText) {
		this.contentText = contentText;
	}

	/**
	 * Returns the parsed MIME type of the response (without parameters).
	 *
	 * @return the parsed MIME type, or {@code null} if the {@code Content-Type} header is absent.
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Stores the parsed MIME type of the response.
	 *
	 * @param contentType the MIME type without parameters; may be {@code null}.
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Returns the raw {@code Content-Type} header value, including any parameters.
	 *
	 * @return the raw header value, or {@code null} if not set.
	 */
	public String getContentTypeString() {
		return contentTypeString;
	}

	/**
	 * Stores the raw {@code Content-Type} header value.
	 *
	 * @param contentTypeString the raw header value; may be {@code null}.
	 */
	public void setContentTypeString(String contentTypeString) {
		this.contentTypeString = contentTypeString;
	}

	/**
	 * Returns an open {@link InputStream} over the response body, when streaming was used.
	 *
	 * @return the body stream, or {@code null} if the body was buffered.
	 */
	public InputStream getContent() {
		return content;
	}

	/**
	 * Stores an open {@link InputStream} over the response body.
	 *
	 * @param content the body stream; may be {@code null}.
	 */
	public void setContent(InputStream content) {
		this.content = content;
	}

	/**
	 * Returns the flattened view of all response headers, keyed by header name.
	 *
	 * @return the header map, or {@code null} if not populated.
	 */
	public Map<String, String> getAllHeaders() {
		return allHeaders;
	}

	/**
	 * Stores the flattened view of all response headers.
	 *
	 * @param allHeaders the header map; may be {@code null}.
	 */
	public void setAllHeaders(Map<String, String> allHeaders) {
		this.allHeaders = allHeaders;
	}
}
