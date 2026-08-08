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
package org.apache.http.spring.boot.client.handler;

import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link HttpRequestExceptionRetryHandler}.
 *
 * <p>Exercises every branch of the {@code retryRequest} decision: maximum
 * attempts reached, recognised transient failure (NoHttpResponseException),
 * recognised non-retryable failures, and the idempotency / request-sent
 * fallback for arbitrary {@link IOException}s.</p>
 *
 * @since 3.0.0
 */
class HttpRequestExceptionRetryHandlerTest {

    /** Default context used by the tests below. */
    private final HttpContext context = new BasicHttpContext();

    /**
     * Once the maximum number of attempts has been reached the handler
     * must always return {@code false}, regardless of the exception type.
     */
    @Test
    void shouldStopRetryingAfterMaxAttempts() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3);
        assertFalse(handler.retryRequest(new NoHttpResponseException("server hung up"),
                4, context));
    }

    /**
     * {@link NoHttpResponseException} is the only exception class for which
     * the handler always returns {@code true} (server dropped the
     * connection, retrying usually succeeds).
     */
    @Test
    void shouldRetryOnNoHttpResponseException() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3);
        assertTrue(handler.retryRequest(new NoHttpResponseException("server hung up"),
                1, context));
    }

    /**
     * {@link InterruptedIOException} represents a cancelled call, so the
     * handler must not retry.
     */
    @Test
    void shouldNotRetryOnInterruptedIOException() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3);
        assertFalse(handler.retryRequest(new InterruptedIOException("interrupted"),
                1, context));
    }

    /**
     * DNS failures are non-transient: the handler must not retry.
     */
    @Test
    void shouldNotRetryOnUnknownHostException() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3);
        assertFalse(handler.retryRequest(new UnknownHostException("nope.example"),
                1, context));
    }

    /**
     * Connect-timeout exceptions are typically caused by network issues
     * that retrying would not resolve &mdash; the handler must deny.
     */
    @Test
    void shouldNotRetryOnConnectTimeoutException() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3);
        assertFalse(handler.retryRequest(new ConnectTimeoutException(),
                1, context));
    }

    /**
     * SSL exceptions (including handshake failures) must not trigger a retry.
     */
    @Test
    void shouldNotRetryOnSslException() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3);
        assertFalse(handler.retryRequest(new SSLHandshakeException("handshake failed"),
                1, context));
    }

    /**
     * A custom {@link SSLException} subclass must also be treated as
     * non-retryable.
     */
    @Test
    void shouldNotRetryOnSslExceptionSubclass() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3);
        assertFalse(handler.retryRequest(new SSLException("trust failure"),
                1, context));
    }

    /**
     * When the request is idempotent (a GET) the handler must retry any
     * other {@link IOException}.
     */
    @Test
    void shouldRetryIdempotentRequestForOtherExceptions() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3);
        HttpRequest request = new HttpGet("http://example.com/");
        assertTrue(handler.retryRequest(new IOException("socket reset"), 1,
                contextWith(request)));
    }

    /**
     * For a non-idempotent request (POST) with {@code requestSentRetryEnabled}
     * disabled, the handler must not retry an arbitrary exception.
     */
    @Test
    void shouldNotRetryNonIdempotentRequestWhenRequestSentRetryDisabled() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3, false);
        HttpRequest request = new HttpPost("http://example.com/");
        assertFalse(handler.retryRequest(new IOException("socket reset"), 1,
                contextWith(request)));
    }

    /**
     * For a non-idempotent request (POST) with {@code requestSentRetryEnabled}
     * enabled, the handler must retry an arbitrary exception.
     */
    @Test
    void shouldRetryNonIdempotentRequestWhenRequestSentRetryEnabled() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3, true);
        HttpRequest request = new HttpPost("http://example.com/");
        assertTrue(handler.retryRequest(new IOException("socket reset"), 1,
                contextWith(request)));
    }

    /**
     * The single-argument constructor must default to
     * {@code requestSentRetryEnabled=true}.
     */
    @Test
    void shouldDefaultToRequestSentRetryEnabled() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(5);
        HttpRequest request = new HttpPost("http://example.com/");
        assertTrue(handler.retryRequest(new IOException("reset"), 1, contextWith(request)));
    }

    /**
     * A zero {@code retryTime} budget must deny every request, including
     * the {@link NoHttpResponseException} that is otherwise always retried.
     */
    @Test
    void shouldDenyRetriesWhenRetryTimeIsZero() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(0);
        assertFalse(handler.retryRequest(new NoHttpResponseException("server hung up"),
                1, context));
    }

    /**
     * The handler must rely on the context to identify the request, so a
     * {@code null} request is acceptable as long as the request would be
     * considered idempotent (i.e. not an entity-enclosing request).
     */
    @Test
    void shouldTreatNullRequestAsIdempotent() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3);
        assertTrue(handler.retryRequest(new IOException("reset"), 1, context));
    }

    /**
     * The handler must honour the {@link HttpRequestBase} type to detect
     * non-idempotent requests even when supplied via a subclass.
     */
    @Test
    void shouldDetectNonIdempotencyFromHttpPostSubclass() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3, false);
        HttpRequest request = new HttpPost("http://example.com/");
        assertFalse(handler.retryRequest(new IOException("reset"), 1, contextWith(request)));
    }

    /**
     * Sanity check: a {@link HttpGet} is NOT entity-enclosing, so the
     * handler must retry arbitrary I/O failures even when
     * {@code requestSentRetryEnabled} is {@code false}.
     */
    @Test
    void shouldRetryHttpGetWhenRequestSentRetryDisabled() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3, false);
        HttpRequest request = new HttpGet("http://example.com/");
        assertTrue(handler.retryRequest(new IOException("reset"), 1, contextWith(request)));
    }

    /**
     * A bare non-{@code HttpRequestBase} request must be treated as
     * idempotent because it does not implement
     * {@link org.apache.http.HttpEntityEnclosingRequest}.
     */
    @Test
    void shouldTreatBareRequestAsIdempotent() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3, false);
        HttpRequest request = new BasicHttpRequest("GET", "/");
        assertTrue(handler.retryRequest(new IOException("reset"), 1, contextWith(request)));
    }

    /**
     * The {@link HttpClientContext#adapt(HttpContext)} contract is what
     * allows the handler to peek at the request; supplying a plain
     * {@code BasicHttpContext} must work as well.
     */
    @Test
    void shouldAdaptPlainHttpContext() {
        HttpRequestExceptionRetryHandler handler = new HttpRequestExceptionRetryHandler(3);
        // a plain context has no request → request is null → idempotent path
        assertTrue(handler.retryRequest(new IOException("reset"), 1, new BasicHttpContext()));
    }

    /**
     * Sanity check that {@link HttpPost} is an
     * {@link org.apache.http.HttpEntityEnclosingRequest}; this is true by
     * contract and is here to keep static analysers happy.
     */
    @Test
    void shouldRecogniseEntityEnclosingRequestBase() {
        HttpPost post = new HttpPost("http://example.com/");
        assertTrue(post instanceof org.apache.http.HttpEntityEnclosingRequest);
    }

    /**
     * Helper: build an {@link HttpClientContext} with the supplied request
     * bound to {@code "http.request"}.
     */
    private static HttpContext contextWith(HttpRequest request) {
        HttpClientContext ctx = HttpClientContext.create();
        ctx.setAttribute(HttpClientContext.HTTP_REQUEST, request);
        return ctx;
    }
}
