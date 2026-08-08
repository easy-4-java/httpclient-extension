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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;

/**
 * Unit tests for {@link HttpResponseGzipInterceptor}.
 *
 * @since 3.0.0
 */
class HttpResponseGzipInterceptorTest {

    /**
     * The interceptor must tolerate a response without an entity.
     */
    @Test
    void shouldTolerateNullEntity() throws IOException, HttpException {
        HttpResponseGzipInterceptor interceptor = new HttpResponseGzipInterceptor();
        HttpResponse response = new org.apache.http.message.BasicHttpResponse(
                new org.apache.http.message.BasicStatusLine(
                        org.apache.http.HttpVersion.HTTP_1_1, 200, "OK"));
        assertDoesNotThrow(() -> interceptor.process(response, new BasicHttpContext()));
    }

    /**
     * The interceptor must leave the response alone when the entity has
     * no {@code Content-Encoding} header.
     */
    @Test
    void shouldTolerateMissingContentEncoding() throws IOException, HttpException {
        HttpResponseGzipInterceptor interceptor = new HttpResponseGzipInterceptor();
        HttpResponse response = newResponse(200, new StringEntity("hello"), null);
        HttpEntity original = response.getEntity();
        interceptor.process(response, new BasicHttpContext());
        assertSame(original, response.getEntity());
    }

    /**
     * A {@code gzip} {@code Content-Encoding} must be replaced with a
     * {@link GzipDecompressingEntity}.
     */
    @Test
    void shouldWrapGzipEncodedEntity() throws IOException, HttpException {
        HttpResponseGzipInterceptor interceptor = new HttpResponseGzipInterceptor();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(new byte[] {0, 1, 2}));
        HttpResponse response = newResponse(200, entity, new BasicHeader("Content-Encoding", "gzip"));
        interceptor.process(response, new BasicHttpContext());
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity() instanceof GzipDecompressingEntity);
    }

    /**
     * A {@code deflate} {@code Content-Encoding} must be replaced with a
     * {@link DeflateDecompressingEntity}.
     */
    @Test
    void shouldWrapDeflateEncodedEntity() throws IOException, HttpException {
        HttpResponseGzipInterceptor interceptor = new HttpResponseGzipInterceptor();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(new byte[] {0, 1, 2}));
        HttpResponse response = newResponse(200, entity, new BasicHeader("Content-Encoding", "deflate"));
        interceptor.process(response, new BasicHttpContext());
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity() instanceof DeflateDecompressingEntity);
    }

    /**
     * Encodings other than {@code gzip} / {@code deflate} must be left
     * untouched.
     */
    @Test
    void shouldLeaveUnrecognisedEncodingAlone() throws IOException, HttpException {
        HttpResponseGzipInterceptor interceptor = new HttpResponseGzipInterceptor();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(new byte[] {0, 1, 2}));
        HttpResponse response = newResponse(200, entity, new BasicHeader("Content-Encoding", "br"));
        HttpEntity original = response.getEntity();
        interceptor.process(response, new BasicHttpContext());
        assertSame(original, response.getEntity());
    }

    /**
     * When the {@code Content-Encoding} lists both {@code deflate} and
     * {@code gzip}, the first element in the header wins. Here
     * {@code deflate} appears first, so
     * {@link DeflateDecompressingEntity} is applied.
     */
    @Test
    void shouldMatchFirstEncodingInHeader() throws IOException, HttpException {
        HttpResponseGzipInterceptor interceptor = new HttpResponseGzipInterceptor();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(new byte[] {0, 1, 2}));
        Header header = new BasicHeader("Content-Encoding", "deflate, gzip");
        HttpResponse response = newResponse(200, entity, header);
        interceptor.process(response, new BasicHttpContext());
        assertTrue(response.getEntity() instanceof DeflateDecompressingEntity);
    }

    /**
     * When {@code gzip} appears first in a multi-valued
     * {@code Content-Encoding} header, it must be selected.
     */
    @Test
    void shouldPreferGzipWhenListedFirst() throws IOException, HttpException {
        HttpResponseGzipInterceptor interceptor = new HttpResponseGzipInterceptor();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(new byte[] {0, 1, 2}));
        Header header = new BasicHeader("Content-Encoding", "gzip, deflate");
        HttpResponse response = newResponse(200, entity, header);
        interceptor.process(response, new BasicHttpContext());
        assertTrue(response.getEntity() instanceof GzipDecompressingEntity);
    }

    /**
     * An empty {@code Content-Encoding} value must be left untouched.
     */
    @Test
    void shouldLeaveEmptyContentEncodingAlone() throws IOException, HttpException {
        HttpResponseGzipInterceptor interceptor = new HttpResponseGzipInterceptor();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(new byte[] {0, 1, 2}));
        HttpResponse response = newResponse(200, entity, new BasicHeader("Content-Encoding", ""));
        HttpEntity original = response.getEntity();
        interceptor.process(response, new BasicHttpContext());
        assertSame(original, response.getEntity());
    }

    /**
     * The interceptor must tolerate a {@code null} context.
     */
    @Test
    void shouldTolerateNullContext() throws IOException, HttpException {
        HttpResponseGzipInterceptor interceptor = new HttpResponseGzipInterceptor();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(new byte[] {0, 1, 2}));
        HttpResponse response = newResponse(200, entity, new BasicHeader("Content-Encoding", "gzip"));
        assertDoesNotThrow(() -> interceptor.process(response, null));
        assertTrue(response.getEntity() instanceof GzipDecompressingEntity);
    }

    /**
     * Encoding matching must be case-insensitive.
     */
    @Test
    void shouldMatchEncodingCaseInsensitively() throws IOException, HttpException {
        HttpResponseGzipInterceptor interceptor = new HttpResponseGzipInterceptor();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(new byte[] {0, 1, 2}));
        HttpResponse response = newResponse(200, entity, new BasicHeader("Content-Encoding", "GZIP"));
        interceptor.process(response, new BasicHttpContext());
        assertTrue(response.getEntity() instanceof GzipDecompressingEntity);
    }

    /**
     * Helper: build a fresh {@link HttpResponse} with the supplied
     * status, entity and content-encoding header.
     */
    private static HttpResponse newResponse(int status, HttpEntity entity, Header contentEncoding) {
        HttpResponse response = new org.apache.http.message.BasicHttpResponse(
                new org.apache.http.message.BasicStatusLine(
                        org.apache.http.HttpVersion.HTTP_1_1, status, "OK"));
        if (entity != null) {
            // Set content-encoding on the entity itself, because the
            // interceptor reads entity.getContentEncoding() not the
            // response-level headers.
            if (contentEncoding != null
                    && entity instanceof org.apache.http.entity.AbstractHttpEntity) {
                ((org.apache.http.entity.AbstractHttpEntity) entity)
                        .setContentEncoding(contentEncoding);
            }
            response.setEntity(entity);
        }
        if (contentEncoding != null) {
            response.setHeader(contentEncoding);
        }
        return response;
    }
}
