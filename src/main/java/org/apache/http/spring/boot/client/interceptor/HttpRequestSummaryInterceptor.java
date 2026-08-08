package org.apache.http.spring.boot.client.interceptor;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * No-op {@link HttpRequestInterceptor} that serves as the base class for
 * interceptors that may eventually emit a request summary.
 *
 * <p>The current implementation does not perform any side effects on the
 * request or the context &mdash; it is a placeholder. Subclasses may
 * override {@link #process(HttpRequest, HttpContext)} to inject logging,
 * metrics or other diagnostic behaviour without changing the surrounding
 * configuration.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see HttpRequestInterceptor
 */
public class HttpRequestSummaryInterceptor implements HttpRequestInterceptor {

	/**
	 * Does nothing &mdash; provided so that subclasses can override the hook
	 * or so that the class can be wired into a pipeline as a marker.
	 *
	 * @param request the current request.
	 * @param context the current execution context.
	 * @throws HttpException never thrown by the default implementation; declared for compatibility.
	 * @throws IOException never thrown by the default implementation; declared for compatibility.
	 */
	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
	}
}
