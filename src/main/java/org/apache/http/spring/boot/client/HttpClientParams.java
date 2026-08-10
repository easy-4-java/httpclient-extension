package org.apache.http.spring.boot.client;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Enumeration of well-known configuration keys consumed by the easy-4-java
 * HttpClient integration.
 *
 * <p>Each constant bundles a configuration-property name with a sensible
 * default value, which makes the parameter set easy to expose through
 * Spring-Boot {@code @ConfigurationProperties} or similar mechanisms. Callers
 * typically resolve a property via {@link #getName()} and use {@link #getDefault()}
 * as a fallback when the value is not supplied by the environment.</p>
 *
 * <p>The {@link #valueOfIgnoreCase(String, String)} helper exists to support
 * case-insensitive lookup from configuration files, where the user might write
 * the parameter in lowercase or mixed case.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
public enum HttpClientParams {

	/** Master switch for the easy-4-java {@code HttpClient} auto-configuration (default {@code false}). */
	HTTP_CONNECTION_MANAGER("http.connection.manager", "false"),
	/** Keep-alive duration, in milliseconds, applied to pooled connections (default {@code 30000}). */
	HTTP_CONNECTION_KEEPALIVE("http.connection.keepAlive", "30000"),
	/** Maximum size of the HttpClient connection pool (default {@code 20}). */
	HTTP_CONNECTION_MAX_POOLSIZE("http.connection.maxPoolSize", "20"),
	/** Number of automatic retry attempts for transient I/O failures (default {@code 5}). */
	HTTP_CONNECTION_RETRY_TIME("http.connection.retryTime", "5"),
	/** Maximum line length, in characters, accepted on the response wire (default {@code 2000}). */
	HTTP_CONNECTION_MAX_LINE_LENGTH("http.connection.max-line-length", "2000"),
	/** Maximum number of header lines accepted in a single response (default {@code 200}). */
	HTTP_CONNECTION_MAX_HEADER_COUNT("http.connection.max-header-count", "200"),
	/** Charset used to decode HTTP wire text (default {@code UTF-8}). */
	HTTP_CONNECTION_CONFIG_CHARSET("http.connection.config-charset", StandardCharsets.UTF_8.toString()),
	/** Whether DNS lookups are skipped because hosts are pre-resolved (default {@code true}). */
	HTTP_CONNECTION_DNS_LOOKUPS_DISABLED("http.connection.dns.lookups-disabled", "true"),
	/** Whether the integration emits Codahale/Dropwizard metrics (default {@code true}). */
	HTTP_CONNECTION_METRICS_DISABLED("http.connection.metrics.disabled", "true"),
	/** Name of the metrics registry the integration reports to (default {@code httpclient}). */
	HTTP_CONNECTION_METRICS_REGISTRYNAME("http.connection.metrics.registryName", "httpclient"),
	/** Whether TCP_NODELAY (Nagle disable) is enabled on every socket (default {@code true}). */
	HTTP_SOCKET_TCPNODELAY("http.socket.tcpNoDelay", "true"),
	/** Socket read timeout, in milliseconds, before the request is aborted (default {@code 5000}). */
	HTTP_SOCKET_SO_TIMEOUT("http.socket.so_timeout", "5000"),
	/** Connect timeout, in milliseconds, before the request is aborted (default {@code 5000}). */
	HTTP_REQUEST_CONNECT_TIMEOUT("http.request.connect_timeout", "5000"),
	/** Per-request socket timeout, in milliseconds, before the request is aborted (default {@code 5000}). */
	HTTP_REQUEST_SOCKET_TIMEOUT("http.request.socket_timeout", "5000"),
	/** Default SSL/TLS protocol negotiated by the client (default {@code TLS}). */
	HTTP_SSL_PROTOCOL("http.ssl.protocol", "TLS");

	/** The configuration-property name, never {@code null}. */
	private final String name;
	/** The default value, mutable so that {@link #valueOfIgnoreCase(String, String)} can override it. */
	private String defaultValue;

	/**
	 * Creates a new constant with the supplied name and default value.
	 *
	 * @param name the configuration-property name; must not be {@code null}.
	 * @param defaultValue the default value, expressed as a string; must not be {@code null}.
	 */
	HttpClientParams(String name, String defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	/**
	 * Returns the configuration-property name associated with this constant.
	 *
	 * @return the non-null property name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the current default value for this constant.
	 *
	 * @return the non-null default value, expressed as a string.
	 */
	public String getDefault() {
		return defaultValue;
	}

	/**
	 * Looks up a constant by its configuration name (case-insensitive) and
	 * overrides its default value with the one supplied.
	 *
	 * <p>This is intended to be used by reflective configuration loaders that
	 * discover parameter names at runtime. The supplied name is upper-cased
	 * using {@link Locale#ENGLISH} and trimmed before lookup.</p>
	 *
	 * @param parameter the parameter name to look up; must match an existing constant after
	 *                  upper-casing and trimming.
	 * @param defaultValue the replacement default value to install on the matching constant.
	 * @return the matching {@link HttpClientParams} constant, with its default value updated.
	 * @throws IllegalArgumentException if no constant matches the supplied name.
	 * @throws NullPointerException if {@code parameter} is {@code null}.
	 */
	static HttpClientParams valueOfIgnoreCase(String parameter, String defaultValue) {
		HttpClientParams parm = valueOf(parameter.toUpperCase(Locale.ENGLISH).trim());
		parm.defaultValue = defaultValue;
		return parm;
	}
}
