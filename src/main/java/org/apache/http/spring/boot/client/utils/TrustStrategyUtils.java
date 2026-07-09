package org.apache.http.spring.boot.client.utils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;

public final class TrustStrategyUtils {

	private static final TrustStrategy ACCEPT_ALL = new SSLTrustStrategy(false);
	private static final TrustStrategy CHECK_VALIDITY = new SSLTrustStrategy(true);
	private static final TrustStrategy SELF_SIGNED = TrustSelfSignedStrategy.INSTANCE;

	private TrustStrategyUtils() {
	}

	public static TrustStrategy getSelfSignedTrustStrategy() {
		return SELF_SIGNED;
	}

	public static TrustStrategy getAcceptAllTrustStrategy() {
		return ACCEPT_ALL;
	}

	public static TrustStrategy getValidateCertificateTrustStrategy() {
		return CHECK_VALIDITY;
	}

	private static class SSLTrustStrategy implements TrustStrategy {

		private final boolean checkValidity;

		SSLTrustStrategy(boolean checkValidity) {
			this.checkValidity = checkValidity;
		}

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
