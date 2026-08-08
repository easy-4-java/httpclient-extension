package org.apache.http.spring.boot.client.handler;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.protocol.HttpContext;

/**
 * {@link HttpRequestRetryHandler} that retries a request when the server drops
 * the connection mid-response, and additionally retries non-idempotent
 * requests when explicitly enabled by configuration.
 *
 * <p>The handler treats the following exception classes as
 * <em>non-retryable</em>:</p>
 * <ul>
 *   <li>{@link InterruptedIOException} &mdash; the caller's thread was
 *       interrupted and should not be retried.</li>
 *   <li>{@link UnknownHostException} &mdash; DNS resolution failed, retrying is
 *       unlikely to succeed.</li>
 *   <li>{@link ConnectTimeoutException} &mdash; the connection could not be
 *       established within the configured timeout.</li>
 *   <li>{@link SSLException} &mdash; a TLS-level error occurred, which is
 *       typically configuration-related and not transient.</li>
 * </ul>
 *
 * <p>For any other exception the handler applies the default Apache
 * HttpClient policy: retries are allowed for idempotent requests (anything
 * that is not an {@link HttpEntityEnclosingRequest}) and, when
 * {@code requestSentRetryEnabled} is {@code true}, also for non-idempotent
 * requests whose body has already been sent.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see HttpRequestRetryHandler
 */
public class HttpRequestExceptionRetryHandler implements HttpRequestRetryHandler {

	/** Maximum number of retry attempts before giving up; never negative. */
	private final int retryTime;
	/** Whether non-idempotent requests whose body was already sent may be retried. */
	private final boolean requestSentRetryEnabled;

	/**
	 * Convenience constructor that uses the default policy of allowing retries
	 * for non-idempotent requests whose body has already been sent.
	 *
	 * @param retryTime the maximum number of retry attempts; negative values are treated as zero.
	 */
	public HttpRequestExceptionRetryHandler(int retryTime) {
		this(retryTime, true);
	}

	/**
	 * Full constructor.
	 *
	 * @param retryTime the maximum number of retry attempts.
	 * @param requestSentRetryEnabled whether non-idempotent requests may be
	 *                                retried after the body has been sent.
	 */
	public HttpRequestExceptionRetryHandler(int retryTime, boolean requestSentRetryEnabled) {
		this.retryTime = retryTime;
		this.requestSentRetryEnabled = requestSentRetryEnabled;
	}

	/**
	 * Decides whether the failed request should be retried.
	 *
	 * @param exception the {@link IOException} thrown by the last execution.
	 * @param executionCount the number of times the request has been executed so far (1-based).
	 * @param context the current {@link HttpContext}, used to inspect the
	 *                request being executed.
	 * @return {@code true} if the request should be retried, {@code false} otherwise.
	 */
	@Override
	public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
		if (executionCount >= retryTime) {
			return false;
		}
		if (exception instanceof NoHttpResponseException) {
			return true;
		}
		if (exception instanceof InterruptedIOException) {
			return false;
		}
		if (exception instanceof UnknownHostException) {
			return false;
		}
		if (exception instanceof ConnectTimeoutException) {
			return false;
		}
		if (exception instanceof SSLException) {
			return false;
		}
		HttpClientContext clientContext = HttpClientContext.adapt(context);
		HttpRequest request = clientContext.getRequest();
		boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
		return idempotent || requestSentRetryEnabled;
	}
}
