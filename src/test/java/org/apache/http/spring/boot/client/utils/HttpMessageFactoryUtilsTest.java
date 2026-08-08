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

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.config.MessageConstraints;
import org.apache.http.impl.conn.DefaultHttpResponseParserFactory;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.message.BasicLineParser;
import org.apache.http.util.CharArrayBuffer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link HttpMessageFactoryUtils}.
 *
 * @since 3.0.0
 */
class HttpMessageFactoryUtilsTest {

    /**
     * The response parser factory must be a non-null instance.
     */
    @Test
    void shouldExposeResponseParserFactory() {
        HttpMessageParserFactory<HttpResponse> factory = HttpMessageFactoryUtils.getResponseParserFactory();
        assertNotNull(factory);
    }

    /**
     * The request writer factory must be a non-null instance.
     */
    @Test
    void shouldExposeRequestWriterFactory() {
        HttpMessageWriterFactory<HttpRequest> factory = HttpMessageFactoryUtils.getRequestWriterFactory();
        assertNotNull(factory);
    }

    /**
     * Both factory methods must return the cached singleton every time.
     */
    @Test
    void shouldReturnCachedSingletons() {
        assertSame(HttpMessageFactoryUtils.getResponseParserFactory(),
                HttpMessageFactoryUtils.getResponseParserFactory());
        assertSame(HttpMessageFactoryUtils.getRequestWriterFactory(),
                HttpMessageFactoryUtils.getRequestWriterFactory());
    }

    /**
     * The class must be {@code final} and have a private no-arg
     * constructor that is reachable only via reflection.
     */
    @Test
    void shouldHideConstructor() throws Exception {
        assertTrue(Modifier.isFinal(HttpMessageFactoryUtils.class.getModifiers()));
        Constructor<HttpMessageFactoryUtils> ctor =
                HttpMessageFactoryUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
        } catch (InvocationTargetException unexpected) {
            fail("unexpected: " + unexpected.getCause());
        }
    }

    /**
     * The request writer factory must be a {@code DefaultHttpRequestWriterFactory}
     * or a compatible subclass.
     */
    @Test
    void shouldExposeDefaultRequestWriterFactoryType() {
        HttpMessageWriterFactory<HttpRequest> factory = HttpMessageFactoryUtils.getRequestWriterFactory();
        assertTrue(factory instanceof DefaultHttpRequestWriterFactory);
    }

    /**
     * The response parser factory must be a custom subclass of the
     * default Apache factory (so that the lenient parser is wired in).
     */
    @Test
    void shouldExposeCustomResponseParserFactoryType() {
        HttpMessageParserFactory<HttpResponse> factory = HttpMessageFactoryUtils.getResponseParserFactory();
        assertTrue(factory instanceof DefaultHttpResponseParserFactory);
        assertNotSame(DefaultHttpResponseParserFactory.INSTANCE, factory);
    }

    /**
     * Public surface must expose exactly two factory methods.
     */
    @Test
    void shouldExposeTwoFactoryMethods() {
        long count = Arrays.stream(HttpMessageFactoryUtils.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .count();
        assertEquals(2L, count);
    }

    /**
     * The strict parser must reject a header line without a colon.
     */
    @Test
    void strictParserShouldRejectMalformedLine() {
        BasicLineParser strict = new BasicLineParser();
        CharArrayBuffer buf = new CharArrayBuffer(16);
        buf.append("X-No-Colon");
        assertThrows(ParseException.class, () -> strict.parseHeader(buf));
    }

    /**
     * Sanity check that a freshly created parser instance is not the
     * same object as the default Apache factory.
     */
    @Test
    void customFactoryShouldNotBeApacheDefault() {
        assertNotSame(DefaultHttpResponseParserFactory.INSTANCE,
                HttpMessageFactoryUtils.getResponseParserFactory());
    }

    /**
     * The lenient parser should not reject any line, even short ones.
     * We verify the contract by exercising the parser through a real
     * HTTP/1.1 response wire format.
     */
    @Test
    void shouldParseHttpResponseWithoutRejecting() throws Exception {
        HttpMessageParserFactory<HttpResponse> factory = HttpMessageFactoryUtils.getResponseParserFactory();
        String wire = "HTTP/1.1 200 OK\r\n" +
                "X-Test: value\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n";
        SessionInputBufferImpl buffer = newSessionInputBuffer(wire);
        HttpMessageParser<HttpResponse> parser = factory.create(buffer, MessageConstraints.DEFAULT);
        HttpResponse response = parser.parse();
        assertNotNull(response);
        assertEquals(200, response.getStatusLine().getStatusCode());
    }

    /**
     * The parser must tolerate a malformed header (no colon) by falling
     * back to a {@link org.apache.http.message.BasicHeader} with a null
     * value rather than throwing.
     */
    @Test
    void shouldLenientlyParseMalformedHeader() throws Exception {
        HttpMessageParserFactory<HttpResponse> factory = HttpMessageFactoryUtils.getResponseParserFactory();
        String wire = "HTTP/1.1 200 OK\r\n" +
                "X-No-Colon\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n";
        SessionInputBufferImpl buffer = newSessionInputBuffer(wire);
        HttpMessageParser<HttpResponse> parser = factory.create(buffer, MessageConstraints.DEFAULT);
        HttpResponse response = parser.parse();
        assertNotNull(response);
        // The malformed header is preserved as-is.
        org.apache.http.Header header = response.getFirstHeader("X-No-Colon");
        assertNotNull(header);
        assertEquals(null, header.getValue());
    }

    /**
     * The factory must accept an arbitrary {@link MessageConstraints} and
     * still produce a non-null parser.
     */
    @Test
    void shouldAcceptMessageConstraints() throws Exception {
        HttpMessageParserFactory<HttpResponse> factory = HttpMessageFactoryUtils.getResponseParserFactory();
        SessionInputBufferImpl buffer = newSessionInputBuffer("HTTP/1.1 200 OK\r\n\r\n");
        HttpMessageParser<HttpResponse> parser = factory.create(buffer, MessageConstraints.DEFAULT);
        assertNotNull(parser);
    }

    /**
     * Build an in-memory {@link SessionInputBufferImpl} bound to a
     * {@link ByteArrayInputStream} carrying the supplied wire bytes.
     */
    private static SessionInputBufferImpl newSessionInputBuffer(String wire) throws Exception {
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        // Use the (HttpTransportMetricsImpl, int) public constructor.
        Constructor<SessionInputBufferImpl> ctor = SessionInputBufferImpl.class
                .getConstructor(HttpTransportMetricsImpl.class, int.class);
        SessionInputBufferImpl buffer = ctor.newInstance(metrics, 1024);
        buffer.bind(new ByteArrayInputStream(wire.getBytes(StandardCharsets.US_ASCII)));
        return buffer;
    }
}
