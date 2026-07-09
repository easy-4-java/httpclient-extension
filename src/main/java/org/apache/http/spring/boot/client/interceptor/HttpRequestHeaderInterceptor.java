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

public class HttpRequestHeaderInterceptor implements HttpRequestInterceptor {

	private final Map<String, String> headers;

	public HttpRequestHeaderInterceptor(Properties properties) {
		this.headers = propertiesToMap(properties);
	}

	public HttpRequestHeaderInterceptor(Map<String, String> headers) {
		this.headers = Objects.nonNull(headers) ? headers : Collections.<String, String>emptyMap();
	}

	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			if (Objects.nonNull(entry.getKey()) && Objects.nonNull(entry.getValue())) {
				request.addHeader(entry.getKey(), entry.getValue());
			}
		}
	}

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
