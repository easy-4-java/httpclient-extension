package org.apache.http.spring.boot.client.utils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;

/**
 * Factory methods for the most common {@link TrustStrategy} implementations
 * used by the easy-4-java HttpClient integration.
 *
 * <p>All strategies are thread-safe singletons because they are stateless.
 * The exposed strategies are:</p>
 * <ul>
 *   <li>{@link #getAcceptAllTrustStrategy()} &mdash; trusts every certificate,
 *       disabling both chain verification and validity checks. Intended for
 *       non-production environments.</li>
 *   <li>{@link #getValidateCertificateTrustStrategy()} &mdash; trusts every
 *       certificate but still enforces the validity window of each entry.</li>
 *   <li>{@link #getSelfSignedTrustStrategy()} &mdash; delegates to Apache
 *       HttpClient's {@link TrustSelfSignedStrategy} for self-signed
 *       certificates.</li>
 * </ul>
 *
 * <p>This class is not instantiable.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see TrustStrategy
 */
public final class TrustStrategyUtils {

	/** Trusts every certificate without validity checks; never {@code null}. */
	private static final TrustStrategy ACCEPT_ALL = new SSLTrustStrategy(false);
	/** Trusts every certificate but enforces the validity window; never {@code null}. */
	private static final TrustStrategy CHECK_VALIDITY = new SSLTrustStrategy(true);
	/** Delegates to {@link TrustSelfSignedStrategy}; never {@code null}. */
	private static final TrustStrategy SELF_SIGNED = TrustSelfSignedStrategy.INSTANCE;

	/**
	 * Private constructor &mdash; the class is a static utility holder.
	 */
	private TrustStrategyUtils() {
	}

	/**
	 * Returns a {@link TrustStrategy} that accepts any self-signed certificate.
	 *
	 * @return the shared self-signed strategy; never {@code null}.
	 */
	public static TrustStrategy getSelfSignedTrustStrategy() {
		return SELF_SIGNED;
	}

	/**
	 * Returns a {@link TrustStrategy} that accepts every certificate without
	 * any further validation. Use only for development or testing.
	 *
	 * @return the shared accept-all strategy; never {@code null}.
	 */
	public static TrustStrategy getAcceptAllTrustStrategy() {
		return ACCEPT_ALL;
	}

	/**
	 * Returns a {@link TrustStrategy} that accepts every certificate but
	 * enforces the validity window via {@link X509Certificate#checkValidity()}.
	 *
	 * @return the shared validate-only strategy; never {@code null}.
	 */
	public static TrustStrategy getValidateCertificateTrustStrategy() {
		return CHECK_VALIDITY;
	}

	/**
	 * Internal {@link TrustStrategy} that conditionally performs validity
	 * checks before accepting a chain.
	 */
	private static class SSLTrustStrategy implements TrustStrategy {

		/** Whether {@link X509Certificate#checkValidity()} should be invoked. */
		private final boolean checkValidity;

		/**
		 * Creates a new strategy.
		 *
		 * @param checkValidity when {@code true}, the strategy verifies the
		 *                      validity window of each certificate in the chain.
		 */
		SSLTrustStrategy(boolean checkValidity) {
			this.checkValidity = checkValidity;
		}

		/**
		 * Returns {@code true} after (optionally) verifying the validity window
		 * of every certificate in the supplied chain.
		 *
		 * @param certificates the chain presented by the peer; may be empty.
		 * @param authType the authentication type (e.g. {@code "RSA"}).
		 * @return always {@code true} once the validity check has succeeded.
		 * @throws CertificateException if {@code checkValidity} is {@code true}
		 *                              and at least one certificate is expired or not yet valid.
		 */
		@Override
		public boolean isTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
			if (checkValidity) {
				for (X509Certificate certificate : certificates) {
					certificate.checkValidity();
				}
			}
			return true;
		}
	}
}
