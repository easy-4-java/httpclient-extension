package org.apache.http.spring.boot.client.utils;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Objects;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContexts;

/**
 * Factory helpers for creating {@link SSLContext} instances in a way that is
 * consistent with the rest of the easy-4-java HttpClient integration.
 *
 * <p>The class adapts the Apache HttpClient {@link SSLContexts} builder so
 * that:</p>
 * <ul>
 *   <li>Single {@link KeyManager} / {@link TrustManager} arguments are
 *       transparently wrapped into array form (and {@code null} entries are
 *       preserved as {@code null} arrays).</li>
 *   <li>{@link GeneralSecurityException} raised by the underlying builder is
 *       wrapped in an {@link IOException} for callers that prefer a single
 *       checked exception type.</li>
 *   <li>Key-store-driven contexts are constructed via
 *       {@link org.apache.http.ssl.SSLContextBuilder#loadTrustMaterial(KeyStore, TrustStrategy)}
 *       so that both an in-memory {@link KeyStore} and a file-based store
 *       (with a password) are supported.</li>
 * </ul>
 *
 * <p>This class is not instantiable.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see SSLContext
 * @see TrustManager
 * @see KeyManager
 */
public final class SSLContextUtils {

	/**
	 * Private constructor &mdash; the class is a static utility holder.
	 */
	private SSLContextUtils() {
	}

	/**
	 * Creates an {@link SSLContext} configured with a single key manager and
	 * a single trust manager. Either argument may be {@code null}.
	 *
	 * @param protocol the SSL/TLS protocol to negotiate (e.g. {@code "TLS"} or {@code "TLSv1.3"}).
	 * @param keyManager the key manager, or {@code null} to leave unauthenticated.
	 * @param trustManager the trust manager, or {@code null} to use the platform default.
	 * @return a fresh {@link SSLContext} ready to be used.
	 * @throws IOException if the underlying SSL builder fails; the original
	 *                     {@link GeneralSecurityException} is attached as the cause.
	 */
	public static SSLContext createSSLContext(String protocol, KeyManager keyManager, TrustManager trustManager)
			throws IOException {
		return createSSLContext(protocol,
				Objects.isNull(keyManager) ? null : new KeyManager[] { keyManager },
				Objects.isNull(trustManager) ? null : new TrustManager[] { trustManager });
	}

	/**
	 * Creates an {@link SSLContext} configured with arrays of key managers
	 * and trust managers.
	 *
	 * @param protocol the SSL/TLS protocol to negotiate.
	 * @param keyManagers the key managers, or {@code null} to leave unauthenticated.
	 * @param trustManagers the trust managers, or {@code null} to use the platform default.
	 * @return a fresh {@link SSLContext} ready to be used.
	 * @throws IOException if the underlying SSL builder fails; the original
	 *                     {@link GeneralSecurityException} is attached as the cause.
	 */
	public static SSLContext createSSLContext(String protocol, KeyManager[] keyManagers, TrustManager[] trustManagers)
			throws IOException {
		try {
			SSLContext ctx = SSLContexts.custom().useProtocol(protocol).build();
			ctx.init(keyManagers, trustManagers, null);
			return ctx;
		} catch (GeneralSecurityException e) {
			IOException ioe = new IOException("Could not initialize SSL context");
			ioe.initCause(e);
			throw ioe;
		}
	}

	/**
	 * Creates an {@link SSLContext} backed by an in-memory {@link KeyStore}
	 * and an Apache HttpClient {@link TrustStrategy}.
	 *
	 * @param keystore the key store supplying trust material; must not be {@code null}.
	 * @param trustStrategy the trust strategy that decides which entries of
	 *                      {@code keystore} are acceptable; must not be {@code null}.
	 * @return a fresh {@link SSLContext} ready to be used.
	 * @throws IOException if the underlying SSL builder fails; the original
	 *                     {@link GeneralSecurityException} is attached as the cause.
	 */
	public static SSLContext createSSLContext(KeyStore keystore, TrustStrategy trustStrategy) throws IOException {
		try {
			return SSLContexts.custom().loadTrustMaterial(keystore, trustStrategy).build();
		} catch (GeneralSecurityException e) {
			IOException ioe = new IOException("Could not initialize SSL context");
			ioe.initCause(e);
			throw ioe;
		}
	}

	/**
	 * Creates an {@link SSLContext} that loads trust material from a key
	 * store file using the supplied password and trust strategy.
	 *
	 * @param protocol the SSL/TLS protocol to negotiate.
	 * @param keystore the key-store file to load; must exist and be readable.
	 * @param storePassword the password protecting the key store, as a {@code String}
	 *                      (converted internally to a character array).
	 * @param trustStrategy the trust strategy applied to the loaded material.
	 * @return a fresh {@link SSLContext} ready to be used.
	 * @throws IOException if the underlying SSL builder fails; the original
	 *                     {@link GeneralSecurityException} is attached as the cause.
	 */
	public static SSLContext createSSLContext(String protocol, File keystore, String storePassword,
			TrustStrategy trustStrategy) throws IOException {
		try {
			return SSLContexts.custom().useProtocol(protocol)
					.loadTrustMaterial(keystore, storePassword.toCharArray(), trustStrategy).build();
		} catch (GeneralSecurityException e) {
			IOException ioe = new IOException("Could not initialize SSL context");
			ioe.initCause(e);
			throw ioe;
		}
	}

	/**
	 * Returns the platform-default {@link SSLContext} as exposed by
	 * {@link SSLContexts#createSystemDefault()}.
	 *
	 * @return the JVM-default SSL context; never {@code null}.
	 */
	public static SSLContext createDefaultSSLContext() {
		return SSLContexts.createSystemDefault();
	}
}
