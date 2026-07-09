package org.apache.http.spring.boot.client;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public enum HttpClientParams {

	HTTP_CONNECTION_MANAGER("http.connection.manager", "false"),
	HTTP_CONNECTION_KEEPALIVE("http.connection.keepAlive", "30000"),
	HTTP_CONNECTION_MAX_POOLSIZE("http.connection.maxPoolSize", "20"),
	HTTP_CONNECTION_RETRY_TIME("http.connection.retryTime", "5"),
	HTTP_CONNECTION_MAX_LINE_LENGTH("http.connection.max-line-length", "2000"),
	HTTP_CONNECTION_MAX_HEADER_COUNT("http.connection.max-header-count", "200"),
	HTTP_CONNECTION_CONFIG_CHARSET("http.connection.config-charset", StandardCharsets.UTF_8.toString()),
	HTTP_CONNECTION_DNS_LOOKUPS_DISABLED("http.connection.dns.lookups-disabled", "true"),
	HTTP_CONNECTION_METRICS_DISABLED("http.connection.metrics.disabled", "true"),
	HTTP_CONNECTION_METRICS_REGISTRYNAME("http.connection.metrics.registryName", "httpclient"),
	HTTP_SOCKET_TCPNODELAY("http.socket.tcpNoDelay", "true"),
	HTTP_SOCKET_SO_TIMEOUT("http.socket.so_timeout", "5000"),
	HTTP_REQUEST_CONNECT_TIMEOUT("http.request.connect_timeout", "5000"),
	HTTP_REQUEST_SOCKET_TIMEOUT("http.request.socket_timeout", "5000"),
	HTTP_SSL_PROTOCOL("http.ssl.protocol", "TLS");

	private final String name;
	private String defaultValue;

	HttpClientParams(String name, String defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	public String getName() {
		return name;
	}

	public String getDefault() {
		return defaultValue;
	}

	static HttpClientParams valueOfIgnoreCase(String parameter, String defaultValue) {
		HttpClientParams parm = valueOf(parameter.toUpperCase(Locale.ENGLISH).trim());
		parm.defaultValue = defaultValue;
		return parm;
	}
}
