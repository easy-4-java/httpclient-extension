package org.apache.http.spring.boot.client.interceptor;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * {@link HttpRequestInterceptor} that adds an {@code Accept-Encoding: gzip}
 * header to every outgoing request, unless the caller has already supplied
 * one.
 *
 * <p>The interceptor pairs naturally with
 * {@link HttpResponseGzipInterceptor} so that the full request/response
 * cycle is compressed transparently &mdash; the request advertises
 * {@code gzip} as an accepted encoding, the upstream may return a
 * {@code gzip} body, and the response interceptor decodes it back to plain
 * bytes before delivering it to the application.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see HttpRequestInterceptor
 * @see HttpResponseGzipInterceptor
 */
public class HttpRequestGzipInterceptor implements HttpRequestInterceptor {

	/**
	 * Adds {@code Accept-Encoding: gzip} to the request when the header is
	 * not already present.
	 *
	 * @param request the current request.
	 * @param context the current execution context (unused).
	 * @throws HttpException never thrown by this implementation; declared for compatibility.
	 * @throws IOException never thrown by this implementation; declared for compatibility.
	 */
	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
		if (!request.containsHeader("Accept-Encoding")) {
			request.addHeader("Accept-Encoding", "gzip");
		}
	}
}
