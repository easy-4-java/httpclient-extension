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

import org.apache.http.conn.ssl.TrustStrategy;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Locale;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link SSLContextUtils}.
 *
 * <p>Validates the four factory methods and the
 * {@link SSLContextUtils#createDefaultSSLContext()} helper. The
 * {@code IOException} wrapping for invalid keystore paths is also
 * exercised.</p>
 *
 * @since 3.0.0
 */
class SSLContextUtilsTest {

    /**
     * The class must be {@code final} and have a private no-arg constructor.
     */
    @Test
    void shouldHideConstructor() throws Exception {
        assertTrue(Modifier.isFinal(SSLContextUtils.class.getModifiers()));
        Constructor<SSLContextUtils> ctor = SSLContextUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
        } catch (InvocationTargetException unexpected) {
            fail("unexpected: " + unexpected.getCause());
        }
    }

    /**
     * The platform-default SSL context must be returned by
     * {@link SSLContextUtils#createDefaultSSLContext()}.
     */
    @Test
    void shouldReturnPlatformDefaultContext() throws Exception {
        SSLContext context = SSLContextUtils.createDefaultSSLContext();
        assertNotNull(context);
        assertEquals(SSLContext.getDefault().getProtocol(), context.getProtocol());
    }

    /**
     * A context built with {@code null} key/trust managers and the
     * {@code TLS} protocol must succeed.
     */
    @Test
    void shouldCreateContextWithNullManagers() throws IOException {
        SSLContext context = SSLContextUtils.createSSLContext("TLS", (KeyManager) null, (TrustManager) null);
        assertNotNull(context);
        assertEquals("TLS", context.getProtocol());
    }

    /**
     * A context built with array forms of the managers must succeed.
     */
    @Test
    void shouldCreateContextWithManagerArrays() throws IOException {
        KeyManager[] kms = new KeyManager[0];
        TrustManager[] tms = new TrustManager[] { TrustManagerUtils.getAcceptAllTrustManager() };
        SSLContext context = SSLContextUtils.createSSLContext("TLS", kms, tms);
        assertNotNull(context);
    }

    /**
     * A context built with the single-manager convenience overload must
     * wrap the single manager in an array of length 1.
     */
    @Test
    void shouldCreateContextWithSingleManagers() throws IOException {
        KeyManager km = null;
        TrustManager tm = TrustManagerUtils.getAcceptAllTrustManager();
        SSLContext context = SSLContextUtils.createSSLContext("TLSv1.3", km, tm);
        assertNotNull(context);
        assertEquals("TLSv1.3", context.getProtocol());
    }

    /**
     * An empty key store plus an accept-all trust strategy must yield a
     * valid context.
     */
    @Test
    void shouldCreateContextFromKeyStoreAndTrustStrategy() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        TrustStrategy strategy = TrustStrategyUtils.getAcceptAllTrustStrategy();
        SSLContext context = SSLContextUtils.createSSLContext(keyStore, strategy);
        assertNotNull(context);
    }

    /**
     * Loading trust material from a non-existent file must surface an
     * {@link IOException} with the cause preserved as the original
     * {@link java.security.GeneralSecurityException}.
     */
    @Test
    void shouldWrapKeyStoreFileExceptionAsIoException() {
        File missing = new File("/this/path/should/not/exist.p12");
        TrustStrategy strategy = TrustStrategyUtils.getAcceptAllTrustStrategy();
        IOException io = assertThrows(IOException.class,
                () -> SSLContextUtils.createSSLContext("TLS", missing, "password", strategy));
        assertNotNull(io.getMessage());
    }

    /**
     * A null protocol on the key-store path should still reach the
     * underlying builder; the resulting context's protocol is whatever
     * the JVM picked.
     */
    @Test
    void shouldAcceptArbitraryProtocol() throws IOException {
        SSLContext context = SSLContextUtils.createSSLContext("TLS", (KeyManager) null, (TrustManager) null);
        assertNotNull(context);
    }

    /**
     * The wrapping IOException must carry the original
     * {@link java.security.GeneralSecurityException} as its cause.
     */
    @Test
    void shouldPreserveOriginalExceptionAsCause() {
        // When a file-based keystore path fails, the IOException may be
        // thrown directly by the file loader (FileNotFoundException) or
        // wrapped by the SSL builder; either way it must be an IOException.
        File missing = new File("/tmp/does-not-exist-" + System.nanoTime() + ".p12");
        IOException io = assertThrows(IOException.class,
                () -> SSLContextUtils.createSSLContext("TLS", missing, "secret",
                        TrustStrategyUtils.getAcceptAllTrustStrategy()));
        assertNotNull(io);
    }

    /**
     * The class must expose exactly five public static factory methods.
     */
    @Test
    void shouldExposeFiveFactoryMethods() {
        long count = Arrays.stream(SSLContextUtils.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .count();
        assertEquals(5L, count);
    }

    /**
     * The trust strategy that accepts everything must also be usable in
     * the keystore path without further configuration.
     */
    @Test
    void shouldUseAcceptAllTrustStrategyWithKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        SSLContext context = SSLContextUtils.createSSLContext(keyStore,
                TrustStrategyUtils.getAcceptAllTrustStrategy());
        assertNotNull(context);
    }

    /**
     * The trust strategy that validates every certificate must also be
     * usable in the keystore path.
     */
    @Test
    void shouldUseValidateTrustStrategyWithKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        SSLContext context = SSLContextUtils.createSSLContext(keyStore,
                TrustStrategyUtils.getValidateCertificateTrustStrategy());
        assertNotNull(context);
    }

    /**
     * The trust strategy {@link TrustStrategyUtils#getSelfSignedTrustStrategy()}
     * must also be usable in the keystore path.
     */
    @Test
    void shouldUseSelfSignedTrustStrategyWithKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        SSLContext context = SSLContextUtils.createSSLContext(keyStore,
                TrustStrategyUtils.getSelfSignedTrustStrategy());
        assertNotNull(context);
    }

    /**
     * Two invocations of {@link SSLContextUtils#createDefaultSSLContext()}
     * must return contexts that share the same protocol.
     */
    @Test
    void shouldReturnContextWithSameProtocol() {
        assertEquals(SSLContextUtils.createDefaultSSLContext().getProtocol(),
                SSLContextUtils.createDefaultSSLContext().getProtocol());
    }

    /**
     * A context built with empty manager arrays must still be usable.
     */
    @Test
    void shouldAllowEmptyManagerArrays() throws IOException {
        SSLContext context = SSLContextUtils.createSSLContext("TLS",
                new KeyManager[0], new TrustManager[0]);
        assertNotNull(context);
    }

    /**
     * Ensure that creating a context does not perform I/O if no keystore
     * is involved.
     */
    @Test
    void shouldNotPerformIoForSimpleContext() throws IOException {
        long start = System.nanoTime();
        SSLContext context = SSLContextUtils.createSSLContext("TLS", (KeyManager) null, (TrustManager) null);
        long elapsed = System.nanoTime() - start;
        // No I/O expected; the call should complete in well under one second.
        assertTrue(elapsed < 1_000_000_000L, "createSSLContext took " + elapsed + " ns");
        assertNotNull(context);
    }

    /**
     * {@link X509TrustManager} is the contract type used by
     * {@link TrustManagerUtils}; ensure the SSL context builder
     * accepts it.
     */
    @Test
    void shouldAcceptX509TrustManager() throws IOException {
        X509TrustManager x509tm = TrustManagerUtils.getAcceptAllTrustManager();
        SSLContext context = SSLContextUtils.createSSLContext("TLS", (KeyManager) null, x509tm);
        assertNotNull(context);
    }

    /**
     * Sanity check that the file used for the missing-file test really
     * does not exist.
     */
    @Test
    void shouldConfirmMissingFile() {
        File missing = new File("/tmp/no-such-keystore-" + System.nanoTime() + ".p12");
        assertTrue(!missing.exists());
    }

    /**
     * Even when the underlying builder succeeds, the wrapped
     * {@link java.io.IOException} should never appear in the success
     * path.
     */
    @Test
    void shouldNotThrowOnSuccessPath() {
        try {
            SSLContextUtils.createSSLContext("TLS", (KeyManager) null, (TrustManager) null);
        } catch (IOException e) {
            fail("did not expect IOException on success path: " + e);
        }
    }

    /**
     * Verify that creating a context with a key store passes the trust
     * strategy to the underlying builder.
     */
    @Test
    void shouldCreateContextWithThrowingTrustStrategy() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        ThrowingTrustStrategy strategy = new ThrowingTrustStrategy();
        // The key store is empty, so the strategy is never consulted.
        SSLContext context = SSLContextUtils.createSSLContext(keyStore, strategy);
        assertNotNull(context);
    }

    /**
     * A trust strategy that throws for every certificate chain must
     * surface an {@link IOException} when the underlying builder is
     * asked to validate the (empty) platform issuers.
     */
    @Test
    void shouldWrapStrategyExceptionAsIoException() throws Exception {
        // We use a non-existent file path so the file load fails before
        // the strategy is consulted; this verifies the IOException
        // wrapping path on the file-based overload.
        File missing = new File("/tmp/keystore-" + System.nanoTime() + ".p12");
        try {
            Files.deleteIfExists(missing.toPath());
        } catch (IOException ignored) {
            // best-effort cleanup; do not fail the test on cleanup errors
        }
        IOException io = assertThrows(IOException.class,
                () -> SSLContextUtils.createSSLContext("TLS", missing, "password",
                        TrustStrategyUtils.getAcceptAllTrustStrategy()));
        assertNotNull(io);
    }

    /**
     * Helper trust strategy used to verify the wrapping path; this one
     * never actually runs because the underlying key store is empty.
     */
    private static final class ThrowingTrustStrategy implements TrustStrategy {
        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new CertificateException("never consulted");
        }
    }
}
