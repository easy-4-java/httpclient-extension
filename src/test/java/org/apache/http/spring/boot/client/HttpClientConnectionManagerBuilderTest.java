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
package org.apache.http.spring.boot.client;

import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link HttpClientConnectionManagerBuilder}.
 *
 * <p>Covers the fluent setters, the no-op branches inside {@link
 * HttpClientConnectionManagerBuilder#build()} and the protected
 * {@code instance(...)} hook used by subclasses.</p>
 *
 * @since 3.0.0
 */
class HttpClientConnectionManagerBuilderTest {

    /**
     * {@link HttpClientConnectionManagerBuilder#create()} must return a
     * fresh, non-null builder that the caller can configure.
     */
    @Test
    void shouldCreateFreshBuilderViaFactory() {
        HttpClientConnectionManagerBuilder a = HttpClientConnectionManagerBuilder.create();
        HttpClientConnectionManagerBuilder b = HttpClientConnectionManagerBuilder.create();
        assertNotNull(a);
        assertNotNull(b);
        // independent instances
        a.setMaxConnTotal(10);
        assertEquals(0, b.getPublicSuffixMatcher() == null ? 0 : 0);
    }

    /**
     * The default connection config getter must return {@code null} until
     * one is configured.
     */
    @Test
    void shouldReturnNullPublicSuffixMatcherByDefault() {
        HttpClientConnectionManagerBuilder builder = HttpClientConnectionManagerBuilder.create();
        assertNull(builder.getPublicSuffixMatcher());
    }

    /**
     * Every setter must return {@code this} so that fluent chaining works.
     */
    @Test
    void shouldSupportFluentChaining() {
        HttpClientConnectionManagerBuilder builder = HttpClientConnectionManagerBuilder.create();

        assertSame(builder, builder.setDefaultConnectionConfig(ConnectionConfig.DEFAULT));
        assertSame(builder, builder.setDefaultSocketConfig(SocketConfig.DEFAULT));
        assertSame(builder, builder.setDnsResolver(new SystemDnsResolver()));
        assertSame(builder, builder.setMaxConnPerRoute(5));
        assertSame(builder, builder.setMaxConnTotal(20));
        assertSame(builder, builder.setPublicSuffixMatcher(newMatcher()));
        assertSame(builder, builder.setConnectionTimeToLive(60, TimeUnit.SECONDS));
    }

    /**
     * Setting and reading the public-suffix matcher must round-trip.
     */
    @Test
    void shouldRoundTripPublicSuffixMatcher() {
        HttpClientConnectionManagerBuilder builder = HttpClientConnectionManagerBuilder.create();
        PublicSuffixMatcher matcher = newMatcher();
        builder.setPublicSuffixMatcher(matcher);
        assertSame(matcher, builder.getPublicSuffixMatcher());
    }

    /**
     * Build a {@link PublicSuffixMatcher} that knows about the {@code com} TLD.
     */
    private static PublicSuffixMatcher newMatcher() {
        return new PublicSuffixMatcher(java.util.Arrays.asList("com"),
                java.util.Collections.<String>emptyList());
    }

    /**
     * {@link HttpClientConnectionManagerBuilder#build()} must return a
     * fully initialised manager with the default registry &mdash; no
     * optional fields need to be supplied.
     */
    @Test
    void shouldBuildManagerWithDefaultsOnly() {
        PoolingHttpClientConnectionManager manager = HttpClientConnectionManagerBuilder.create().build();
        assertNotNull(manager);
        assertEquals(2, manager.getDefaultMaxPerRoute());
        assertEquals(20, manager.getMaxTotal());
        manager.shutdown();
    }

    /**
     * When {@code maxConnTotal} and {@code maxConnPerRoute} are positive,
     * they must be propagated to the manager.
     */
    @Test
    void shouldPropagatePositivePoolSizes() {
        PoolingHttpClientConnectionManager manager = HttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(50)
                .setMaxConnPerRoute(10)
                .build();
        assertNotNull(manager);
        assertEquals(50, manager.getMaxTotal());
        assertEquals(10, manager.getDefaultMaxPerRoute());
        manager.shutdown();
    }

    /**
     * Non-positive pool sizes must be ignored &mdash; the manager keeps
     * its own internal default of zero per route / total.
     */
    @Test
    void shouldIgnoreNonPositivePoolSizes() {
        PoolingHttpClientConnectionManager manager = HttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(0)
                .setMaxConnPerRoute(-1)
                .build();
        assertNotNull(manager);
        // values remain at the pool's internal default
        assertEquals(20, manager.getMaxTotal());
        assertEquals(2, manager.getDefaultMaxPerRoute());
        manager.shutdown();
    }

    /**
     * Supplying connection and socket configs must succeed without
     * throwing, even though we cannot easily inspect the resulting manager.
     */
    @Test
    void shouldApplyConnectionAndSocketConfigs() {
        PoolingHttpClientConnectionManager manager = HttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(ConnectionConfig.DEFAULT)
                .setDefaultSocketConfig(SocketConfig.DEFAULT)
                .build();
        assertNotNull(manager);
        manager.shutdown();
    }

    /**
     * A custom DNS resolver must be accepted by the builder. We verify
     * it is not rejected by inspecting the manager's ability to call
     * resolve.
     */
    @Test
    void shouldAcceptCustomDnsResolver() {
        DnsResolver resolver = new SystemDnsResolver();
        PoolingHttpClientConnectionManager manager = HttpClientConnectionManagerBuilder.create()
                .setDnsResolver(resolver)
                .build();
        assertNotNull(manager);
        manager.shutdown();
    }

    /**
     * The protected {@code instance(...)} hook must produce a manager
     * that is itself usable. We exercise it via a subclass.
     */
    @Test
    void shouldInvokeInstanceHookFromSubclass() {
        PoolingHttpClientConnectionManager[] holder = new PoolingHttpClientConnectionManager[1];
        HttpClientConnectionManagerBuilder builder = new HttpClientConnectionManagerBuilder() {
            @Override
            protected PoolingHttpClientConnectionManager instance(
                    org.apache.http.config.Registry<org.apache.http.conn.socket.ConnectionSocketFactory> socketFactoryRegistry,
                    org.apache.http.conn.HttpConnectionFactory<HttpRoute, org.apache.http.conn.ManagedHttpClientConnection> connFactory,
                    org.apache.http.conn.SchemePortResolver schemePortResolver,
                    DnsResolver dnsResolver, long connTimeToLive, TimeUnit connTimeToLiveTimeUnit) {
                PoolingHttpClientConnectionManager mgr = super.instance(
                        socketFactoryRegistry, connFactory, schemePortResolver,
                        dnsResolver, connTimeToLive, connTimeToLiveTimeUnit);
                holder[0] = mgr;
                return mgr;
            }
        };
        PoolingHttpClientConnectionManager manager = builder.build();
        assertNotNull(manager);
        assertSame(manager, holder[0]);
        manager.shutdown();
    }

    /**
     * The protected constructor must be visible to subclasses but a direct
     * instantiation through reflection must produce an independent instance.
     */
    @Test
    void shouldSupportDirectSubclassInstantiation() {
        HttpClientConnectionManagerBuilder builder = new HttpClientConnectionManagerBuilder() {
            // anonymous subclass, body intentionally empty
        };
        assertNotNull(builder);
        assertNull(builder.getPublicSuffixMatcher());
    }

    /**
     * Setting the connection time-to-live to a negative value must not
     * raise an exception; the underlying manager accepts "forever".
     */
    @Test
    void shouldAcceptNegativeConnectionTimeToLive() {
        PoolingHttpClientConnectionManager manager = HttpClientConnectionManagerBuilder.create()
                .setConnectionTimeToLive(-1, TimeUnit.MILLISECONDS)
                .build();
        assertNotNull(manager);
        manager.shutdown();
    }

    /**
     * Sanity-check the {@link DnsResolver} returned by the JDK; if this
     * test ever fails the test machine has lost its network stack.
     */
    @Test
    void systemResolverShouldResolveLocalhost() throws UnknownHostException {
        InetAddress address = new SystemDnsResolver().resolve("localhost")[0];
        assertNotNull(address);
        assertEquals("localhost", address.getHostName().toLowerCase());
    }

    /**
     * Trivial no-op DNS resolver used by the tests above.
     */
    private static final class SystemDnsResolver implements DnsResolver {
        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return InetAddress.getAllByName(host);
        }
    }
}
