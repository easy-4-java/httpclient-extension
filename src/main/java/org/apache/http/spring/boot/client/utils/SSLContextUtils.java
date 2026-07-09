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

public final class SSLContextUtils {

	private SSLContextUtils() {
	}

	public static SSLContext createSSLContext(String protocol, KeyManager keyManager, TrustManager trustManager)
			throws IOException {
		return createSSLContext(protocol,
				Objects.isNull(keyManager) ? null : new KeyManager[] { keyManager },
				Objects.isNull(trustManager) ? null : new TrustManager[] { trustManager });
	}

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

	public static SSLContext createSSLContext(KeyStore keystore, TrustStrategy trustStrategy) throws IOException {
		try {
			return SSLContexts.custom().loadTrustMaterial(keystore, trustStrategy).build();
		} catch (GeneralSecurityException e) {
			IOException ioe = new IOException("Could not initialize SSL context");
			ioe.initCause(e);
			throw ioe;
		}
	}

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

	public static SSLContext createDefaultSSLContext() {
		return SSLContexts.createSystemDefault();
	}
}
