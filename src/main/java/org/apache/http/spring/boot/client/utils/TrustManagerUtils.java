package org.apache.http.spring.boot.client.utils;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Factory methods for the most common {@link X509TrustManager} implementations
 * used by the easy-4-java HttpClient integration.
 *
 * <p>Two ready-made managers are provided:</p>
 * <ul>
 *   <li>{@link #getAcceptAllTrustManager()} &mdash; trusts every certificate,
 *       disabling both chain verification and validity checks. Intended for
 *       non-production environments.</li>
 *   <li>{@link #getValidateServerCertificateTrustManager()} &mdash; trusts
 *       every certificate but still enforces the validity window of the
 *       server certificate chain.</li>
 * </ul>
 *
 * <p>For production-grade trust, callers should obtain a key-store-backed
 * manager via {@link #getDefaultTrustManager(KeyStore)} or use the JVM's
 * default trust store.</p>
 *
 * <p>This class is not instantiable.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see X509TrustManager
 * @see TrustManagerFactory
 */
public final class TrustManagerUtils {

	/** Reusable empty issuer array, returned by every lenient trust manager. */
	private static final X509Certificate[] EMPTY_X509CERTIFICATE_ARRAY = new X509Certificate[] {};
	/** Trusts every certificate without validity checks; never {@code null}. */
	private static final X509TrustManager ACCEPT_ALL = new TrustManager(false);
	/** Trusts every certificate but enforces validity of server chains; never {@code null}. */
	private static final X509TrustManager CHECK_SERVER_VALIDITY = new TrustManager(true);

	/**
	 * Private constructor &mdash; the class is a static utility holder.
	 */
	private TrustManagerUtils() {
	}

	/**
	 * Returns a trust manager that accepts every certificate without further
	 * validation. Use only for development or testing.
	 *
	 * @return the shared accept-all manager; never {@code null}.
	 */
	public static X509TrustManager getAcceptAllTrustManager() {
		return ACCEPT_ALL;
	}

	/**
	 * Returns a trust manager that accepts every certificate but enforces
	 * the validity window of every server certificate.
	 *
	 * @return the shared validate-server-validity manager; never {@code null}.
	 */
	public static X509TrustManager getValidateServerCertificateTrustManager() {
		return CHECK_SERVER_VALIDITY;
	}

	/**
	 * Builds a key-store-backed trust manager using the JVM's default
	 * {@link TrustManagerFactory} algorithm.
	 *
	 * @param keyStore the key store to initialise the factory with; must not be {@code null}.
	 * @return the first {@link X509TrustManager} produced by the factory; never {@code null}.
	 * @throws GeneralSecurityException if the factory cannot be initialised.
	 * @throws IndexOutOfBoundsException if the resulting factory exposes no managers.
	 */
	public static X509TrustManager getDefaultTrustManager(KeyStore keyStore) throws GeneralSecurityException {
		String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory instance = TrustManagerFactory.getInstance(defaultAlgorithm);
		instance.init(keyStore);
		return (X509TrustManager) instance.getTrustManagers()[0];
	}

	/**
	 * Internal {@link X509TrustManager} that conditionally performs validity
	 * checks on the server chain before accepting it.
	 */
	private static class TrustManager implements X509TrustManager {

		/** Whether {@link X509Certificate#checkValidity()} should be invoked on the server chain. */
		private final boolean checkServerValidity;

		/**
		 * Creates a new manager.
		 *
		 * @param checkServerValidity when {@code true}, the manager verifies the
		 *                           validity window of every server certificate.
		 */
		TrustManager(boolean checkServerValidity) {
			this.checkServerValidity = checkServerValidity;
		}

		/**
		 * Always accepts the client chain &mdash; this manager is intended for
		 * outbound clients, which rarely authenticate the local end of the
		 * connection.
		 *
		 * @param certificates the certificates presented by the client.
		 * @param authType the authentication type (e.g. {@code "RSA"}).
		 */
		@Override
		public void checkClientTrusted(X509Certificate[] certificates, String authType) {
		}

		/**
		 * Validates the server chain and, when configured, verifies the
		 * validity window of every certificate.
		 *
		 * @param certificates the certificates presented by the server.
		 * @param authType the authentication type (e.g. {@code "RSA"}).
		 * @throws CertificateException if validity checking is enabled and
		 *                              a certificate is expired or not yet valid.
		 */
		@Override
		public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
			if (checkServerValidity) {
				for (X509Certificate certificate : certificates) {
					certificate.checkValidity();
				}
			}
		}

		/**
		 * Returns an empty issuer array, signalling that no specific issuers
		 * are trusted.
		 *
		 * @return a shared empty array.
		 */
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return EMPTY_X509CERTIFICATE_ARRAY;
		}
	}
}
