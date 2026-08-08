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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link HttpRequestSummaryInterceptor}.
 *
 * @since 3.0.0
 */
class HttpRequestSummaryInterceptorTest {

    /**
     * The interceptor must expose a public no-arg constructor and be
     * safe to instantiate.
     */
    @Test
    void shouldInstantiate() {
        HttpRequestSummaryInterceptor interceptor = new HttpRequestSummaryInterceptor();
        assertNotNull(interceptor);
    }

    /**
     * The default implementation must not throw and must not modify the
     * request &mdash; calling it with a fresh request leaves the
     * request's header set empty.
     */
    @Test
    void shouldNotModifyRequest() throws IOException, HttpException {
        HttpRequestSummaryInterceptor interceptor = new HttpRequestSummaryInterceptor();
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());
        assertTrue(request.getAllHeaders().length == 0);
    }

    /**
     * The interceptor must tolerate a {@code null} context and not throw.
     */
    @Test
    void shouldTolerateNullContext() throws IOException, HttpException {
        HttpRequestSummaryInterceptor interceptor = new HttpRequestSummaryInterceptor();
        HttpRequest request = new BasicHttpRequest("GET", "/");
        assertDoesNotThrow(() -> interceptor.process(request, null));
    }

    /**
     * Multiple invocations on the same request must be safe.
     */
    @Test
    void shouldBeIdempotent() throws IOException, HttpException {
        HttpRequestSummaryInterceptor interceptor = new HttpRequestSummaryInterceptor();
        HttpRequest request = new BasicHttpRequest("POST", "/api/v1/orders");
        for (int i = 0; i < 5; i++) {
            interceptor.process(request, new BasicHttpContext());
        }
        assertTrue(request.getAllHeaders().length == 0);
    }

    /**
     * A subclass may override {@code process} to add behaviour; we
     * verify this contract is honoured.
     */
    @Test
    void shouldSupportSubclassOverride() throws IOException, HttpException {
        HttpRequestSummaryInterceptor interceptor = new HttpRequestSummaryInterceptor() {
            @Override
            public void process(HttpRequest req, org.apache.http.protocol.HttpContext ctx) {
                req.addHeader("X-Summary", "yes");
            }
        };
        HttpRequest request = new BasicHttpRequest("GET", "/");
        interceptor.process(request, new BasicHttpContext());
        assertNotNull(request.getFirstHeader("X-Summary"));
        assertTrue("yes".equals(request.getFirstHeader("X-Summary").getValue()));
    }
}
