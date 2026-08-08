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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link HttpRequestGzipInterceptor}.
 *
 * @since 3.0.0
 */
class HttpRequestGzipInterceptorTest {

    /**
     * A fresh request must gain the {@code Accept-Encoding: gzip} header.
     */
    @Test
    void shouldAddAcceptEncodingHeader() throws IOException, HttpException {
        HttpRequestGzipInterceptor interceptor = new HttpRequestGzipInterceptor();
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());
        assertNotNull(request.getFirstHeader("Accept-Encoding"));
        assertEquals("gzip", request.getFirstHeader("Accept-Encoding").getValue());
    }

    /**
     * A request that already has an {@code Accept-Encoding} header must
     * not be modified.
     */
    @Test
    void shouldNotOverrideExistingAcceptEncoding() throws IOException, HttpException {
        HttpRequestGzipInterceptor interceptor = new HttpRequestGzipInterceptor();
        HttpRequest request = new BasicHttpRequest("GET", "/");
        request.addHeader("Accept-Encoding", "deflate");
        interceptor.process(request, new BasicHttpContext());
        assertEquals("deflate", request.getFirstHeader("Accept-Encoding").getValue());
    }

    /**
     * The interceptor must tolerate a {@code null} context.
     */
    @Test
    void shouldTolerateNullContext() throws IOException, HttpException {
        HttpRequestGzipInterceptor interceptor = new HttpRequestGzipInterceptor();
        HttpRequest request = new BasicHttpRequest("GET", "/");
        assertDoesNotThrow(() -> interceptor.process(request, null));
        assertEquals("gzip", request.getFirstHeader("Accept-Encoding").getValue());
    }

    /**
     * The header must be added to requests of every method.
     */
    @Test
    void shouldHandlePostRequests() throws IOException, HttpException {
        HttpRequestGzipInterceptor interceptor = new HttpRequestGzipInterceptor();
        HttpRequest request = new BasicHttpRequest("POST", "/api/v1/orders");
        interceptor.process(request, new BasicHttpContext());
        assertEquals("gzip", request.getFirstHeader("Accept-Encoding").getValue());
    }

    /**
     * The interceptor must be idempotent: calling it twice must not
     * produce duplicate {@code Accept-Encoding} headers.
     */
    @Test
    void shouldBeIdempotent() throws IOException, HttpException {
        HttpRequestGzipInterceptor interceptor = new HttpRequestGzipInterceptor();
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());
        interceptor.process(request, new BasicHttpContext());
        assertEquals(1, request.getHeaders("Accept-Encoding").length);
    }
}
