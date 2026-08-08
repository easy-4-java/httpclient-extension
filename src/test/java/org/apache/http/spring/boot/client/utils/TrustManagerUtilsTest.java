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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link TrustManagerUtils}.
 *
 * @since 3.0.0
 */
class TrustManagerUtilsTest {

    /**
     * The accept-all manager must be non-null and not throw on the
     * server check (it does not call {@code checkValidity}).
     */
    @Test
    void shouldExposeAcceptAllManager() throws CertificateException {
        X509TrustManager manager = TrustManagerUtils.getAcceptAllTrustManager();
        assertNotNull(manager);
        manager.checkServerTrusted(new X509Certificate[0], "RSA");
        manager.checkClientTrusted(new X509Certificate[0], "RSA");
    }

    /**
     * The validate-server manager must not throw on an empty chain.
     */
    @Test
    void shouldExposeValidateServerManager() throws CertificateException {
        X509TrustManager manager = TrustManagerUtils.getValidateServerCertificateTrustManager();
        assertNotNull(manager);
        manager.checkServerTrusted(new X509Certificate[0], "RSA");
    }

    /**
     * In validate mode the manager must call {@code checkValidity} on the
     * supplied server chain &mdash; a stub that throws is propagated.
     */
    @Test
    void shouldPropagateValidityFailure() {
        X509TrustManager manager = TrustManagerUtils.getValidateServerCertificateTrustManager();
        X509Certificate stub = throwingCertificate();
        assertThrows(CertificateException.class,
                () -> manager.checkServerTrusted(new X509Certificate[] { stub }, "RSA"));
    }

    /**
     * The validate manager must invoke {@code checkValidity} for every
     * certificate in the chain, not just the first one.
     */
    @Test
    void shouldCheckEveryCertificateInChain() {
        X509TrustManager manager = TrustManagerUtils.getValidateServerCertificateTrustManager();
        X509Certificate ok = okCertificate();
        X509Certificate bad = throwingCertificate();
        assertThrows(CertificateException.class,
                () -> manager.checkServerTrusted(new X509Certificate[] { ok, bad }, "RSA"));
    }

    /**
     * The accept-all manager must not call {@code checkValidity} on the
     * supplied chain, even if the stub would fail it.
     */
    @Test
    void shouldNotCheckValidityInAcceptAllMode() throws CertificateException {
        X509TrustManager manager = TrustManagerUtils.getAcceptAllTrustManager();
        manager.checkServerTrusted(new X509Certificate[] { throwingCertificate() }, "RSA");
    }

    /**
     * The accept-all manager must trust every client certificate, even
     * with a chain that would normally fail validation.
     */
    @Test
    void shouldAcceptAnyClientCertificate() throws CertificateException {
        X509TrustManager manager = TrustManagerUtils.getAcceptAllTrustManager();
        manager.checkClientTrusted(new X509Certificate[] { throwingCertificate() }, "RSA");
    }

    /**
     * The validate-server manager must trust any client certificate
     * because the trust manager is for outbound clients.
     */
    @Test
    void shouldAcceptAnyClientCertificateInValidateMode() throws CertificateException {
        X509TrustManager manager = TrustManagerUtils.getValidateServerCertificateTrustManager();
        manager.checkClientTrusted(new X509Certificate[] { throwingCertificate() }, "RSA");
    }

    /**
     * The {@link X509TrustManager#getAcceptedIssuers()} method must
     * return a (possibly empty) array, never {@code null}.
     */
    @Test
    void shouldReturnEmptyIssuerArray() {
        X509Certificate[] issuers = TrustManagerUtils.getAcceptAllTrustManager().getAcceptedIssuers();
        assertNotNull(issuers);
        assertEquals(0, issuers.length);

        issuers = TrustManagerUtils.getValidateServerCertificateTrustManager().getAcceptedIssuers();
        assertNotNull(issuers);
        assertEquals(0, issuers.length);
    }

    /**
     * Both factories must return the cached singleton every time.
     */
    @Test
    void shouldReturnCachedSingletons() {
        assertSame(TrustManagerUtils.getAcceptAllTrustManager(),
                TrustManagerUtils.getAcceptAllTrustManager());
        assertSame(TrustManagerUtils.getValidateServerCertificateTrustManager(),
                TrustManagerUtils.getValidateServerCertificateTrustManager());
    }

    /**
     * The two managers must be distinct instances.
     */
    @Test
    void shouldReturnDistinctInstances() {
        assertNotSame(TrustManagerUtils.getAcceptAllTrustManager(),
                TrustManagerUtils.getValidateServerCertificateTrustManager());
    }

    /**
     * The class must be {@code final} and have a private no-arg
     * constructor.
     */
    @Test
    void shouldHideConstructor() throws Exception {
        assertTrue(Modifier.isFinal(TrustManagerUtils.class.getModifiers()));
        Constructor<TrustManagerUtils> ctor =
                TrustManagerUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
        } catch (InvocationTargetException unexpected) {
            fail("unexpected: " + unexpected.getCause());
        }
    }

    /**
     * The class must expose three public static factory methods.
     */
    @Test
    void shouldExposeThreeFactoryMethods() {
        long count = Arrays.stream(TrustManagerUtils.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .count();
        assertEquals(3L, count);
    }

    /**
     * {@link TrustManagerUtils#getDefaultTrustManager(KeyStore)} must
     * initialise the default {@link javax.net.ssl.TrustManagerFactory}
     * with the supplied key store and return its first
     * {@link X509TrustManager}.
     */
    @Test
    void shouldResolveDefaultTrustManagerFromKeyStore() throws Exception {
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance(
                java.security.KeyStore.getDefaultType());
        keyStore.load(null, null);
        X509TrustManager manager = TrustManagerUtils.getDefaultTrustManager(keyStore);
        assertNotNull(manager);
    }

    /**
     * Even with a key store that contains no entries, the default
     * factory produces a non-null manager.
     */
    @Test
    void shouldResolveDefaultTrustManagerWithEmptyKeyStore() throws Exception {
        java.security.KeyStore ks = java.security.KeyStore.getInstance(
                java.security.KeyStore.getDefaultType());
        ks.load(null, null);
        X509TrustManager manager = TrustManagerUtils.getDefaultTrustManager(ks);
        assertNotNull(manager);
        assertNotNull(manager.getAcceptedIssuers());
    }

    private static X509Certificate okCertificate() {
        return new StubX509Certificate(false);
    }

    private static X509Certificate throwingCertificate() {
        return new StubX509Certificate(true);
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
        @Override public byte[] getEncoded() { return new byte[0]; }
        @Override public java.util.List<String> getExtendedKeyUsage() { return null; }
        @Override public synchronized java.util.Collection<java.util.List<?>> getIssuerAlternativeNames() { return null; }
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
        @Override public synchronized java.util.Collection<java.util.List<?>> getSubjectAlternativeNames() { return null; }
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
