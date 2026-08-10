package org.apache.http.spring.boot.client.interceptor;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * {@link HttpRequestInterceptor} that adds a fixed set of headers to every
 * outgoing request, typically used to propagate tracing, authentication or
 * correlation identifiers.
 *
 * <p>The header set is supplied at construction time either as a
 * {@link Map} (preferred, when the values come from Spring configuration) or
 * as a {@link Properties} instance (preferred, when the values come from a
 * {@code .properties} file). Both constructors normalise {@code null} or
 * empty inputs to an empty map so that {@link #process(HttpRequest, HttpContext)}
 * is a safe no-op when no headers have been configured.</p>
 *
 * <p>The interceptor is stateless after construction and therefore safe to
 * share between threads.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see HttpRequestInterceptor
 */
public class HttpRequestHeaderInterceptor implements HttpRequestInterceptor {

	/** The headers to add to every request; never {@code null}. */
	private final Map<String, String> headers;

	/**
	 * Creates a new interceptor from a {@link Properties} instance.
	 *
	 * @param properties the properties to read; {@code null} or empty
	 *                   properties produce an interceptor that does nothing.
	 */
	public HttpRequestHeaderInterceptor(Properties properties) {
		this.headers = propertiesToMap(properties);
	}

	/**
	 * Creates a new interceptor from a {@link Map} of header name to value.
	 *
	 * @param headers the headers to add; {@code null} is treated as an empty map.
	 */
	public HttpRequestHeaderInterceptor(Map<String, String> headers) {
		this.headers = Objects.nonNull(headers) ? headers : Collections.<String, String>emptyMap();
	}

	/**
	 * Adds every configured header to the supplied request, skipping entries
	 * with a {@code null} name or value.
	 *
	 * @param request the current request.
	 * @param context the current execution context (unused).
	 * @throws HttpException never thrown by this implementation; declared for compatibility.
	 * @throws IOException never thrown by this implementation; declared for compatibility.
	 */
	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			if (Objects.nonNull(entry.getKey()) && Objects.nonNull(entry.getValue())) {
				request.addHeader(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Converts a {@link Properties} instance to a {@link Map}, preserving
	 * insertion order via {@link java.util.LinkedHashMap}.
	 *
	 * @param properties the source properties; may be {@code null} or empty.
	 * @return a non-null map, empty when no properties are supplied.
	 */
	private Map<String, String> propertiesToMap(Properties properties) {
		if (Objects.isNull(properties) || properties.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, String> result = new java.util.LinkedHashMap<String, String>();
		for (String name : properties.stringPropertyNames()) {
			result.put(name, properties.getProperty(name));
		}
		return result;
	}
}
