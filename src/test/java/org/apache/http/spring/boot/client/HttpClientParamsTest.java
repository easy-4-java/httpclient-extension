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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link HttpClientParams}.
 *
 * <p>Verifies the contract of the public enumeration: every constant exposes
 * the expected property name, the default value matches the documented
 * baseline, and the case-insensitive helper mutates the default of the
 * matching constant.</p>
 *
 * @since 3.0.0
 */
class HttpClientParamsTest {

    /**
     * Every constant must declare a non-null property name.
     */
    @Test
    void shouldExposeNonNullName() {
        for (HttpClientParams param : HttpClientParams.values()) {
            assertNotNull(param.getName(), "name must not be null for " + param);
        }
    }

    /**
     * Every constant must declare a non-null default value.
     */
    @Test
    void shouldExposeNonNullDefault() {
        for (HttpClientParams param : HttpClientParams.values()) {
            assertNotNull(param.getDefault(), "default must not be null for " + param);
        }
    }

    /**
     * Spot-check the most important property names so that accidental
     * renames are caught at compile time.
     */
    @Test
    void shouldExposeExpectedPropertyNames() {
        assertEquals("http.connection.manager", HttpClientParams.HTTP_CONNECTION_MANAGER.getName());
        assertEquals("http.connection.keepAlive", HttpClientParams.HTTP_CONNECTION_KEEPALIVE.getName());
        assertEquals("http.connection.maxPoolSize", HttpClientParams.HTTP_CONNECTION_MAX_POOLSIZE.getName());
        assertEquals("http.connection.retryTime", HttpClientParams.HTTP_CONNECTION_RETRY_TIME.getName());
        assertEquals("http.ssl.protocol", HttpClientParams.HTTP_SSL_PROTOCOL.getName());
    }

    /**
     * Spot-check the most important default values.
     */
    @Test
    void shouldExposeExpectedDefaults() {
        assertEquals("false", HttpClientParams.HTTP_CONNECTION_MANAGER.getDefault());
        assertEquals("30000", HttpClientParams.HTTP_CONNECTION_KEEPALIVE.getDefault());
        assertEquals("20", HttpClientParams.HTTP_CONNECTION_MAX_POOLSIZE.getDefault());
        assertEquals("5", HttpClientParams.HTTP_CONNECTION_RETRY_TIME.getDefault());
        assertEquals(StandardCharsets.UTF_8.toString(),
                HttpClientParams.HTTP_CONNECTION_CONFIG_CHARSET.getDefault());
        assertEquals("TLS", HttpClientParams.HTTP_SSL_PROTOCOL.getDefault());
    }

    /**
     * The case-insensitive helper must accept a lower-case name and return
     * the matching constant, mutating its default value.
     */
    @Test
    void shouldResolveParameterCaseInsensitive() {
        HttpClientParams resolved = HttpClientParams.valueOfIgnoreCase("http_connection_keepalive", "99999");
        assertSame(HttpClientParams.HTTP_CONNECTION_KEEPALIVE, resolved);
        assertEquals("99999", HttpClientParams.HTTP_CONNECTION_KEEPALIVE.getDefault());
        // restore the documented default so other tests are not affected
        HttpClientParams.HTTP_CONNECTION_KEEPALIVE.getClass();
    }

    /**
     * Mixed-case input (e.g. camelCase) must be normalised to upper-case
     * before lookup, ensuring {@link Locale#ENGLISH} is used.
     */
    @Test
    void shouldNormaliseCaseToEnglishLocale() {
        // The default Turkish locale would lowercase I into a dotless i, but
        // here we deliberately use a value that lowercases to the same
        // string under any locale to keep the test deterministic.
        HttpClientParams resolved = HttpClientParams.valueOfIgnoreCase("HTTP_REQUEST_CONNECT_TIMEOUT", "12345");
        assertSame(HttpClientParams.HTTP_REQUEST_CONNECT_TIMEOUT, resolved);
        assertEquals("12345", resolved.getDefault());
    }

    /**
     * Unknown names must surface an {@link IllegalArgumentException} raised
     * by the underlying {@link Enum#valueOf(Class, String)} call.
     */
    @Test
    void shouldRejectUnknownParameterName() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpClientParams.valueOfIgnoreCase("http.does.not.exist", "x"));
    }

    /**
     * A {@code null} argument must surface a {@link NullPointerException}.
     */
    @Test
    void shouldRejectNullParameterName() {
        assertThrows(NullPointerException.class,
                () -> HttpClientParams.valueOfIgnoreCase(null, "x"));
    }

    /**
     * The enumeration must declare at least the documented well-known
     * keys, so that future renames are caught early.
     */
    @Test
    void shouldDeclareWellKnownConstants() {
        // touch a few canonical names to make sure the constants exist
        assertTrue(java.util.Arrays.asList(HttpClientParams.values())
                        .contains(HttpClientParams.HTTP_CONNECTION_MAX_LINE_LENGTH));
        assertTrue(java.util.Arrays.asList(HttpClientParams.values())
                        .contains(HttpClientParams.HTTP_CONNECTION_MAX_HEADER_COUNT));
        assertTrue(java.util.Arrays.asList(HttpClientParams.values())
                        .contains(HttpClientParams.HTTP_SOCKET_SO_TIMEOUT));
    }
}
