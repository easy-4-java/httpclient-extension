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
package org.apache.http.spring.boot.client.interceptor;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link HttpRequestHeaderInterceptor}.
 *
 * @since 3.0.0
 */
class HttpRequestHeaderInterceptorTest {

    /**
     * The map-based constructor must add every configured header.
     */
    @Test
    void shouldAddAllConfiguredHeaders() throws IOException, HttpException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Trace-Id", "abc-123");
        headers.put("Authorization", "Bearer token");

        HttpRequestHeaderInterceptor interceptor = new HttpRequestHeaderInterceptor(headers);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());

        assertEquals("abc-123", request.getFirstHeader("X-Trace-Id").getValue());
        assertEquals("Bearer token", request.getFirstHeader("Authorization").getValue());
    }

    /**
     * The properties-based constructor must add every property as a header.
     */
    @Test
    void shouldAddAllPropertiesAsHeaders() throws IOException, HttpException {
        Properties properties = new Properties();
        properties.setProperty("X-Service", "checkout");
        properties.setProperty("X-Version", "1.2.3");

        HttpRequestHeaderInterceptor interceptor = new HttpRequestHeaderInterceptor(properties);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());

        assertEquals("checkout", request.getFirstHeader("X-Service").getValue());
        assertEquals("1.2.3", request.getFirstHeader("X-Version").getValue());
    }

    /**
     * A {@code null} map must be treated as an empty map.
     */
    @Test
    void shouldTreatNullMapAsEmpty() throws IOException, HttpException {
        HttpRequestHeaderInterceptor interceptor = new HttpRequestHeaderInterceptor((Map<String, String>) null);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());
        assertEquals(0, request.getAllHeaders().length);
    }

    /**
     * A {@code null} properties instance must be treated as empty.
     */
    @Test
    void shouldTreatNullPropertiesAsEmpty() throws IOException, HttpException {
        HttpRequestHeaderInterceptor interceptor = new HttpRequestHeaderInterceptor((Properties) null);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());
        assertEquals(0, request.getAllHeaders().length);
    }

    /**
     * An empty map must leave the request untouched.
     */
    @Test
    void shouldNotAddAnyHeaderWhenMapIsEmpty() throws IOException, HttpException {
        HttpRequestHeaderInterceptor interceptor = new HttpRequestHeaderInterceptor(Collections.<String, String>emptyMap());
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());
        assertEquals(0, request.getAllHeaders().length);
    }

    /**
     * An empty properties instance must leave the request untouched.
     */
    @Test
    void shouldNotAddAnyHeaderWhenPropertiesIsEmpty() throws IOException, HttpException {
        HttpRequestHeaderInterceptor interceptor = new HttpRequestHeaderInterceptor(new Properties());
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());
        assertEquals(0, request.getAllHeaders().length);
    }

    /**
     * A {@code null} header value must be skipped without throwing.
     */
    @Test
    void shouldSkipNullHeaderValues() throws IOException, HttpException {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-OK", "ok");
        headers.put("X-NULL", null);
        HttpRequestHeaderInterceptor interceptor = new HttpRequestHeaderInterceptor(headers);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());

        assertNotNull(request.getFirstHeader("X-OK"));
        assertNull(request.getFirstHeader("X-NULL"));
        assertEquals(1, request.getAllHeaders().length);
    }

    /**
     * A {@code null} header name must be skipped without throwing.
     */
    @Test
    void shouldSkipNullHeaderNames() throws IOException, HttpException {
        Map<String, String> headers = new HashMap<>();
        headers.put(null, "value");
        HttpRequestHeaderInterceptor interceptor = new HttpRequestHeaderInterceptor(headers);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        assertDoesNotThrow(() -> interceptor.process(request, new BasicHttpContext()));
    }

    /**
     * The interceptor must tolerate a {@code null} context.
     */
    @Test
    void shouldTolerateNullContext() throws IOException, HttpException {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Test", "1");
        HttpRequestHeaderInterceptor interceptor = new HttpRequestHeaderInterceptor(headers);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, null);
        assertEquals("1", request.getFirstHeader("X-Test").getValue());
    }

    /**
     * The properties-based constructor must preserve the order of the
     * keys by using a {@link LinkedHashMap} internally.
     */
    @Test
    void shouldPreservePropertyInsertionOrder() throws IOException, HttpException {
        Properties properties = new Properties();
        properties.setProperty("A", "1");
        properties.setProperty("B", "2");
        properties.setProperty("C", "3");

        HttpRequestHeaderInterceptor interceptor = new HttpRequestHeaderInterceptor(properties);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());

        org.apache.http.Header[] headers = request.getAllHeaders();
        assertEquals(3, headers.length);
        assertEquals("A", headers[0].getName());
        assertEquals("B", headers[1].getName());
        assertEquals("C", headers[2].getName());
    }

    /**
     * Multiple invocations on the same request must add the same header
     * twice (this is the documented behaviour of {@code addHeader}).
     */
    @Test
    void shouldBeIdempotentOnSameRequest() throws IOException, HttpException {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Idempotent", "yes");
        HttpRequestHeaderInterceptor interceptor = new HttpRequestHeaderInterceptor(headers);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());
        interceptor.process(request, new BasicHttpContext());
        // addHeader appends, so we end up with two headers having the same name.
        assertEquals(2, request.getHeaders("X-Idempotent").length);
    }
}
