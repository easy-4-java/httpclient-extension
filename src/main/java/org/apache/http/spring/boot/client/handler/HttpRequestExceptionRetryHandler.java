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

public class HttpRequestExceptionRetryHandler implements HttpRequestRetryHandler {

	private final int retryTime;
	private final boolean requestSentRetryEnabled;

	public HttpRequestExceptionRetryHandler(int retryTime) {
		this(retryTime, true);
	}

	public HttpRequestExceptionRetryHandler(int retryTime, boolean requestSentRetryEnabled) {
		this.retryTime = retryTime;
		this.requestSentRetryEnabled = requestSentRetryEnabled;
	}

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
