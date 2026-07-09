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

public class HttpClientConnectionManagerBuilder {

	private ConnectionConfig defaultConnectionConfig;
	private SocketConfig defaultSocketConfig;
	private DnsResolver dnsResolver;
	private int maxConnPerRoute;
	private int maxConnTotal;
	private PublicSuffixMatcher publicSuffixMatcher;
	private long connTimeToLive = -1;
	private TimeUnit connTimeToLiveTimeUnit = TimeUnit.MILLISECONDS;

	protected HttpClientConnectionManagerBuilder() {
	}

	public static HttpClientConnectionManagerBuilder create() {
		return new HttpClientConnectionManagerBuilder();
	}

	public HttpClientConnectionManagerBuilder setDefaultConnectionConfig(ConnectionConfig defaultConnectionConfig) {
		this.defaultConnectionConfig = defaultConnectionConfig;
		return this;
	}

	public HttpClientConnectionManagerBuilder setDefaultSocketConfig(SocketConfig defaultSocketConfig) {
		this.defaultSocketConfig = defaultSocketConfig;
		return this;
	}

	public HttpClientConnectionManagerBuilder setDnsResolver(DnsResolver dnsResolver) {
		this.dnsResolver = dnsResolver;
		return this;
	}

	public HttpClientConnectionManagerBuilder setMaxConnPerRoute(int maxConnPerRoute) {
		this.maxConnPerRoute = maxConnPerRoute;
		return this;
	}

	public HttpClientConnectionManagerBuilder setMaxConnTotal(int maxConnTotal) {
		this.maxConnTotal = maxConnTotal;
		return this;
	}

	public HttpClientConnectionManagerBuilder setPublicSuffixMatcher(PublicSuffixMatcher publicSuffixMatcher) {
		this.publicSuffixMatcher = publicSuffixMatcher;
		return this;
	}

	public HttpClientConnectionManagerBuilder setConnectionTimeToLive(long connTimeToLive,
			TimeUnit connTimeToLiveTimeUnit) {
		this.connTimeToLive = connTimeToLive;
		this.connTimeToLiveTimeUnit = connTimeToLiveTimeUnit;
		return this;
	}

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

	protected PoolingHttpClientConnectionManager instance(Registry<ConnectionSocketFactory> socketFactoryRegistry,
			HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory,
			SchemePortResolver schemePortResolver, DnsResolver dnsResolver, long connTimeToLive,
			TimeUnit connTimeToLiveTimeUnit) {
		return new PoolingHttpClientConnectionManager(socketFactoryRegistry, connFactory, schemePortResolver,
				dnsResolver, connTimeToLive, connTimeToLiveTimeUnit);
	}

	public PublicSuffixMatcher getPublicSuffixMatcher() {
		return publicSuffixMatcher;
	}
}
