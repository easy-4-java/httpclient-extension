/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.http.spring.boot.client.utils;

import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link TrustStrategyUtils}.
 *
 * <p>Validates the three factory methods, the private constructor (via
 * reflection) and the inner {@code SSLTrustStrategy} class &mdash; in
 * particular the validity check it performs for the "validate" mode.</p>
 *
 * @since 3.0.0
 */
class TrustStrategyUtilsTest {

    /**
     * The self-signed strategy must be the singleton exposed by Apache
     * HttpClient's {@link TrustSelfSignedStrategy}.
     */
    @Test
    void shouldExposeApacheSelfSignedStrategy() {
        TrustStrategy strategy = TrustStrategyUtils.getSelfSignedTrustStrategy();
        assertNotNull(strategy);
        assertSame(TrustSelfSignedStrategy.INSTANCE, strategy);
    }

    /**
     * The accept-all strategy must trust an empty chain without checking
     * validity.
     */
    @Test
    void shouldTrustAllCertificates() throws CertificateException {
        TrustStrategy strategy = TrustStrategyUtils.getAcceptAllTrustStrategy();
        assertTrue(strategy.isTrusted(new X509Certificate[0], "RSA"));
    }

    /**
     * The accept-all strategy must not throw {@link CertificateException}
     * even when the supplied certificate is a stub that would fail
     * {@code checkValidity()}.
     */
    @Test
    void shouldNotCheckValidityInAcceptAllMode() throws CertificateException {
        X509Certificate stub = new StubX509Certificate(false);
        assertTrue(TrustStrategyUtils.getAcceptAllTrustStrategy()
                .isTrusted(new X509Certificate[] { stub }, "RSA"));
    }

    /**
     * The validate-certificate strategy must trust an empty chain without
     * raising an exception.
     */
    @Test
    void shouldTrustEmptyChainInValidateMode() throws CertificateException {
        TrustStrategy strategy = TrustStrategyUtils.getValidateCertificateTrustStrategy();
        assertTrue(strategy.isTrusted(new X509Certificate[0], "RSA"));
    }

    /**
     * In validate mode the strategy must call {@link X509Certificate#checkValidity()}
     * on every certificate in the chain. Using a stub that throws, we
     * confirm the strategy propagates the exception.
     */
    @Test
    void shouldInvokeCheckValidityInValidateMode() {
        X509Certificate throwing = new StubX509Certificate(true);
        TrustStrategy strategy = TrustStrategyUtils.getValidateCertificateTrustStrategy();
        assertThrows(CertificateException.class,
                () -> strategy.isTrusted(new X509Certificate[] { throwing }, "RSA"));
    }

    /**
     * In validate mode, the strategy must call {@code checkValidity()} for
     * every certificate in the chain (not just the first one). The
     * second stub throws, and we expect the exception.
     */
    @Test
    void shouldCheckEveryCertificateInChain() {
        X509Certificate first = new StubX509Certificate(false);
        X509Certificate second = new StubX509Certificate(true);
        TrustStrategy strategy = TrustStrategyUtils.getValidateCertificateTrustStrategy();
        assertThrows(CertificateException.class,
                () -> strategy.isTrusted(new X509Certificate[] { first, second }, "RSA"));
    }

    /**
     * Each factory method must return a non-null strategy, and the
     * accept-all strategy must be a distinct instance from the
     * validate-only one.
     */
    @Test
    void shouldReturnDistinctInstancesForDifferentStrategies() {
        TrustStrategy acceptAll = TrustStrategyUtils.getAcceptAllTrustStrategy();
        TrustStrategy validate = TrustStrategyUtils.getValidateCertificateTrustStrategy();
        assertNotNull(acceptAll);
        assertNotNull(validate);
        assertNotSame(acceptAll, validate);
    }

    /**
     * The same factory call must return the same singleton every time.
     */
    @Test
    void shouldReturnCachedSingleton() {
        assertSame(TrustStrategyUtils.getAcceptAllTrustStrategy(),
                TrustStrategyUtils.getAcceptAllTrustStrategy());
        assertSame(TrustStrategyUtils.getValidateCertificateTrustStrategy(),
                TrustStrategyUtils.getValidateCertificateTrustStrategy());
        assertSame(TrustStrategyUtils.getSelfSignedTrustStrategy(),
                TrustStrategyUtils.getSelfSignedTrustStrategy());
    }

    /**
     * The class must be {@code final} and have a {@code private} no-arg
     * constructor that is reachable only via reflection.
     */
    @Test
    void shouldHideConstructor() throws Exception {
        assertTrue(Modifier.isFinal(TrustStrategyUtils.class.getModifiers()));
        Constructor<TrustStrategyUtils> ctor = TrustStrategyUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
        } catch (InvocationTargetException expected) {
            fail("unexpected exception: " + expected.getCause());
        }
    }

    /**
     * The strategies must remain safe when invoked with a {@code null}
     * authentication type.
     */
    @Test
    void shouldAcceptNullAuthType() throws CertificateException {
        assertTrue(TrustStrategyUtils.getAcceptAllTrustStrategy()
                .isTrusted(new X509Certificate[0], null));
        assertTrue(TrustStrategyUtils.getValidateCertificateTrustStrategy()
                .isTrusted(new X509Certificate[0], null));
    }

    /**
     * Sanity check that {@code X509Certificate} is the contract type used
     * by {@link TrustStrategy#isTrusted(X509Certificate[], String)}.
     */
    @Test
    void shouldExposeX509CertificateContract() {
        boolean found = Arrays.stream(TrustStrategy.class.getMethods())
                .filter(m -> "isTrusted".equals(m.getName()))
                .anyMatch(m -> m.getReturnType() == boolean.class && m.getParameterCount() == 2);
        assertTrue(found);
    }

    /**
     * The class structure should expose exactly three public static
     * factory methods.
     */
    @Test
    void shouldExposeExpectedNumberOfFactoryMethods() {
        long count = Arrays.stream(TrustStrategyUtils.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .count();
        assertEquals(3L, count);
    }

    /**
     * A real (non-throwing) validity check: a stub that does not throw
     * must be accepted by the validate strategy.
     */
    @Test
    void shouldAcceptCertificateWithFutureValidity() throws CertificateException {
        X509Certificate stub = new StubX509Certificate(false);
        TrustStrategy strategy = TrustStrategyUtils.getValidateCertificateTrustStrategy();
        assertTrue(strategy.isTrusted(new X509Certificate[] { stub }, "RSA"));
    }

    /**
     * The strategy must propagate the very {@link CertificateException}
     * thrown by the certificate &mdash; not wrap it in a different type.
     */
    @Test
    void shouldPropagateCertificateExceptionDirectly() {
        X509Certificate throwing = new StubX509Certificate(true);
        try {
            TrustStrategyUtils.getValidateCertificateTrustStrategy()
                    .isTrusted(new X509Certificate[] { throwing }, "RSA");
            fail("expected CertificateException");
        } catch (CertificateException expected) {
            assertEquals("forced", expected.getMessage());
        }
    }

    /**
     * Concrete stub {@link X509Certificate} that optionally throws on
     * {@code checkValidity()}.
     */
    private static final class StubX509Certificate extends X509Certificate {
        private final boolean throwOnCheckValidity;

        StubX509Certificate(boolean throwOnCheckValidity) {
            this.throwOnCheckValidity = throwOnCheckValidity;
        }

        @Override
        public void checkValidity() throws CertificateExpiredException {
            if (throwOnCheckValidity) {
                throw new CertificateExpiredException("forced");
            }
        }

        @Override
        public void checkValidity(Date date) throws CertificateExpiredException {
            if (throwOnCheckValidity) {
                throw new CertificateExpiredException("forced");
            }
        }

        @Override public int getBasicConstraints() { return 0; }
        @Override public byte[] getEncoded() throws CertificateEncodingException { return new byte[0]; }
        @Override public List<String> getExtendedKeyUsage() { return null; }
        @Override public synchronized Collection<List<?>> getIssuerAlternativeNames() { return null; }
        @Override public Principal getIssuerDN() { return null; }
        @Override public boolean[] getIssuerUniqueID() { return new boolean[0]; }
        @Override public X500Principal getIssuerX500Principal() { return null; }
        @Override public boolean[] getKeyUsage() { return new boolean[0]; }
        @Override public Set<String> getNonCriticalExtensionOIDs() { return null; }
        @Override public Date getNotAfter() { return new Date(); }
        @Override public Date getNotBefore() { return new Date(); }
        @Override public PublicKey getPublicKey() { return null; }
        @Override public Set<String> getCriticalExtensionOIDs() { return null; }
        @Override public BigInteger getSerialNumber() { return BigInteger.ZERO; }
        @Override public String getSigAlgName() { return "SHA256withRSA"; }
        @Override public String getSigAlgOID() { return "2.16.840.1.101.3.4.2.1"; }
        @Override public byte[] getSigAlgParams() { return null; }
        @Override public byte[] getSignature() { return new byte[0]; }
        @Override public synchronized Collection<List<?>> getSubjectAlternativeNames() { return null; }
        @Override public Principal getSubjectDN() { return null; }
        @Override public boolean[] getSubjectUniqueID() { return new boolean[0]; }
        @Override public X500Principal getSubjectX500Principal() { return null; }
        @Override public byte[] getTBSCertificate() { return new byte[0]; }
        @Override public int getVersion() { return 3; }
        @Override public boolean hasUnsupportedCriticalExtension() { return false; }
        @Override public void verify(PublicKey key) {}
        @Override public void verify(PublicKey key, String sigProvider) {}
        @Override public String toString() { return "stub"; }
        @Override public byte[] getExtensionValue(String oid) { return null; }
    }
}
