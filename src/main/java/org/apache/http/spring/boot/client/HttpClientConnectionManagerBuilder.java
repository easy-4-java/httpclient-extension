package org.apache.http.spring.boot.client;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * Builder for {@link PoolingHttpClientConnectionManager} instances used by the
 * Spring-Boot-friendly Apache HttpClient 4.x integration.
 *
 * <p>This builder centralises the most common knobs a Spring Boot application
 * needs to configure when bootstrapping a pooled HTTP client: connection and
 * socket defaults, DNS resolver, pool sizes, public-suffix matcher and
 * connection time-to-live. It returns a fully wired
 * {@link PoolingHttpClientConnectionManager} ready to be plugged into an
 * {@code HttpClientBuilder}.</p>
 *
 * <p>The constructor is protected so that subclasses may extend it (for example
 * to provide a customised socket factory registry). Production code should
 * obtain instances through {@link #create()}. The {@link #instance(...)}
 * method is {@code protected} for the same reason &mdash; tests and subclasses
 * can override it to substitute a custom connection manager implementation.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see PoolingHttpClientConnectionManager
 * @see ConnectionConfig
 * @see SocketConfig
 */
public class HttpClientConnectionManagerBuilder {

	/** Default {@link ConnectionConfig} applied to every pooled connection; may be {@code null}. */
	private ConnectionConfig defaultConnectionConfig;

	/** Default {@link SocketConfig} applied to every pooled socket; may be {@code null}. */
	private SocketConfig defaultSocketConfig;

	/** Custom DNS resolver; when {@code null} the system resolver is used. */
	private DnsResolver dnsResolver;

	/** Maximum number of connections per route; values &le; 0 leave the default in place. */
	private int maxConnPerRoute;

	/** Maximum number of connections in the pool; values &le; 0 leave the default in place. */
	private int maxConnTotal;

	/** Public-suffix matcher used for cookie domain validation; may be {@code null}. */
	private PublicSuffixMatcher publicSuffixMatcher;

	/** Connection time-to-live, in {@link #connTimeToLiveTimeUnit}; {@code -1} means "forever". */
	private long connTimeToLive = -1;

	/** Unit for {@link #connTimeToLive}; defaults to milliseconds. */
	private TimeUnit connTimeToLiveTimeUnit = TimeUnit.MILLISECONDS;

	/**
	 * Protected no-arg constructor used by {@link #create()} and by subclasses
	 * that wish to extend the builder with additional configuration steps.
	 */
	protected HttpClientConnectionManagerBuilder() {
	}

	/**
	 * Creates a new builder pre-populated with default values.
	 *
	 * @return a fresh {@link HttpClientConnectionManagerBuilder} ready to be configured.
	 */
	public static HttpClientConnectionManagerBuilder create() {
		return new HttpClientConnectionManagerBuilder();
	}

	/**
	 * Sets the default {@link ConnectionConfig} applied to every pooled connection.
	 *
	 * @param defaultConnectionConfig the connection config to use, or {@code null} to clear.
	 * @return {@code this} builder for fluent chaining.
	 */
	public HttpClientConnectionManagerBuilder setDefaultConnectionConfig(ConnectionConfig defaultConnectionConfig) {
		this.defaultConnectionConfig = defaultConnectionConfig;
		return this;
	}

	/**
	 * Sets the default {@link SocketConfig} applied to every pooled socket.
	 *
	 * @param defaultSocketConfig the socket config to use, or {@code null} to clear.
	 * @return {@code this} builder for fluent chaining.
	 */
	public HttpClientConnectionManagerBuilder setDefaultSocketConfig(SocketConfig defaultSocketConfig) {
		this.defaultSocketConfig = defaultSocketConfig;
		return this;
	}

	/**
	 * Sets a custom {@link DnsResolver} used by the resulting connection manager.
	 *
	 * @param dnsResolver the DNS resolver, or {@code null} to use the system resolver.
	 * @return {@code this} builder for fluent chaining.
	 */
	public HttpClientConnectionManagerBuilder setDnsResolver(DnsResolver dnsResolver) {
		this.dnsResolver = dnsResolver;
		return this;
	}

	/**
	 * Sets the maximum number of connections allowed per HTTP route.
	 *
	 * @param maxConnPerRoute the per-route maximum; values &le; 0 leave the default in place.
	 * @return {@code this} builder for fluent chaining.
	 */
	public HttpClientConnectionManagerBuilder setMaxConnPerRoute(int maxConnPerRoute) {
		this.maxConnPerRoute = maxConnPerRoute;
		return this;
	}

	/**
	 * Sets the maximum total number of connections kept in the pool.
	 *
	 * @param maxConnTotal the total maximum; values &le; 0 leave the default in place.
	 * @return {@code this} builder for fluent chaining.
	 */
	public HttpClientConnectionManagerBuilder setMaxConnTotal(int maxConnTotal) {
		this.maxConnTotal = maxConnTotal;
		return this;
	}

	/**
	 * Sets the {@link PublicSuffixMatcher} used for cookie domain validation.
	 *
	 * @param publicSuffixMatcher the matcher, or {@code null} to disable explicit matching.
	 * @return {@code this} builder for fluent chaining.
	 */
	public HttpClientConnectionManagerBuilder setPublicSuffixMatcher(PublicSuffixMatcher publicSuffixMatcher) {
		this.publicSuffixMatcher = publicSuffixMatcher;
		return this;
	}

	/**
	 * Sets the connection time-to-live and the time unit in which it is expressed.
	 *
	 * @param connTimeToLive the time-to-live value; pass {@code -1} (or any negative value) to
	 *                       keep connections open indefinitely.
	 * @param connTimeToLiveTimeUnit the unit for {@code connTimeToLive}; must not be {@code null}.
	 * @return {@code this} builder for fluent chaining.
	 */
	public HttpClientConnectionManagerBuilder setConnectionTimeToLive(long connTimeToLive,
			TimeUnit connTimeToLiveTimeUnit) {
		this.connTimeToLive = connTimeToLive;
		this.connTimeToLiveTimeUnit = connTimeToLiveTimeUnit;
		return this;
	}

	/**
	 * Builds a fully configured {@link PoolingHttpClientConnectionManager}.
	 *
	 * <p>The manager is wired with a registry containing the default
	 * {@link PlainConnectionSocketFactory} for {@code http} and
	 * {@link SSLConnectionSocketFactory#getSocketFactory()} for {@code https}.
	 * Optional settings (connection config, socket config, pool sizes) are
	 * applied only when they have non-default values.</p>
	 *
	 * @return a ready-to-use {@link PoolingHttpClientConnectionManager}.
	 * @see #instance(Registry, HttpConnectionFactory, SchemePortResolver, DnsResolver, long, TimeUnit)
	 */
	public PoolingHttpClientConnectionManager build() {
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", SSLConnectionSocketFactory.getSocketFactory())
				.build();
		HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory =
				ManagedHttpClientConnectionFactory.INSTANCE;
		SchemePortResolver schemePortResolver = DefaultSchemePortResolver.INSTANCE;
		PoolingHttpClientConnectionManager connectionManager = instance(socketFactoryRegistry, connFactory,
				schemePortResolver, dnsResolver, connTimeToLive, connTimeToLiveTimeUnit);
		if (Objects.nonNull(defaultConnectionConfig)) {
			connectionManager.setDefaultConnectionConfig(defaultConnectionConfig);
		}
		if (Objects.nonNull(defaultSocketConfig)) {
			connectionManager.setDefaultSocketConfig(defaultSocketConfig);
		}
		if (maxConnTotal > 0) {
			connectionManager.setMaxTotal(maxConnTotal);
		}
		if (maxConnPerRoute > 0) {
			connectionManager.setDefaultMaxPerRoute(maxConnPerRoute);
		}
		return connectionManager;
	}

	/**
	 * Hook for subclasses to substitute a customised connection-manager instance.
	 *
	 * <p>The default implementation simply delegates to the
	 * {@link PoolingHttpClientConnectionManager#PoolingHttpClientConnectionManager(Registry,
	 * HttpConnectionFactory, SchemePortResolver, DnsResolver, long, TimeUnit)}
	 * constructor.</p>
	 *
	 * @param socketFactoryRegistry the socket-factory registry built by {@link #build()}.
	 * @param connFactory the connection factory used by Apache HttpClient.
	 * @param schemePortResolver the scheme/port resolver.
	 * @param dnsResolver the DNS resolver (may be {@code null}).
	 * @param connTimeToLive the connection time-to-live.
	 * @param connTimeToLiveTimeUnit the time unit for {@code connTimeToLive}.
	 * @return a {@link PoolingHttpClientConnectionManager} (or a subclass instance).
	 */
	protected PoolingHttpClientConnectionManager instance(Registry<ConnectionSocketFactory> socketFactoryRegistry,
			HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory,
			SchemePortResolver schemePortResolver, DnsResolver dnsResolver, long connTimeToLive,
			TimeUnit connTimeToLiveTimeUnit) {
		return new PoolingHttpClientConnectionManager(socketFactoryRegistry, connFactory, schemePortResolver,
				dnsResolver, connTimeToLive, connTimeToLiveTimeUnit);
	}

	/**
	 * Returns the {@link PublicSuffixMatcher} that was last configured on this builder.
	 *
	 * @return the configured public-suffix matcher, or {@code null} if none was set.
	 */
	public PublicSuffixMatcher getPublicSuffixMatcher() {
		return publicSuffixMatcher;
	}
}
