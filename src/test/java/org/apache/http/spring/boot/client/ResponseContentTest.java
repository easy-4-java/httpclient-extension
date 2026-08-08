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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link ResponseContent}.
 *
 * <p>Verifies the JavaBean contract: every field is readable and writable
 * via its getter/setter pair, including {@code null} values.</p>
 *
 * @since 3.0.0
 */
class ResponseContentTest {

    /**
     * The encoding accessor must echo the last value written through
     * {@link ResponseContent#setEncoding(String)}.
     */
    @Test
    void shouldRoundTripEncoding() {
        ResponseContent content = new ResponseContent();
        assertNull(content.getEncoding());

        content.setEncoding("UTF-8");
        assertEquals("UTF-8", content.getEncoding());

        content.setEncoding(null);
        assertNull(content.getEncoding());
    }

    /**
     * The byte array accessor must preserve the exact array reference and
     * contents written by the caller.
     */
    @Test
    void shouldRoundTripContentBytes() {
        ResponseContent content = new ResponseContent();
        assertNull(content.getContentBytes());

        byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
        content.setContentBytes(payload);
        assertSame(payload, content.getContentBytes());
        assertArrayEquals(payload, content.getContentBytes());
    }

    /**
     * The status code defaults to {@code 0} and must be settable to any int.
     */
    @Test
    void shouldRoundTripStatusCode() {
        ResponseContent content = new ResponseContent();
        assertEquals(0, content.getStatusCode());

        content.setStatusCode(404);
        assertEquals(404, content.getStatusCode());

        content.setStatusCode(200);
        assertEquals(200, content.getStatusCode());

        content.setStatusCode(-1);
        assertEquals(-1, content.getStatusCode());
    }

    /**
     * The text accessor must round-trip a non-null value.
     */
    @Test
    void shouldRoundTripContentText() {
        ResponseContent content = new ResponseContent();
        content.setContentText("hello world");
        assertEquals("hello world", content.getContentText());
    }

    /**
     * The parsed content type must round-trip independently of the raw
     * {@code Content-Type} string.
     */
    @Test
    void shouldRoundTripContentType() {
        ResponseContent content = new ResponseContent();
        content.setContentType("application/json");
        assertEquals("application/json", content.getContentType());

        content.setContentType(null);
        assertNull(content.getContentType());
    }

    /**
     * The raw content-type string accessor must round-trip values that
     * include parameters such as {@code charset}.
     */
    @Test
    void shouldRoundTripContentTypeString() {
        ResponseContent content = new ResponseContent();
        content.setContentTypeString("text/html; charset=UTF-8");
        assertEquals("text/html; charset=UTF-8", content.getContentTypeString());
    }

    /**
     * The {@link InputStream} accessor must round-trip the exact reference
     * that was provided by the caller.
     */
    @Test
    void shouldRoundTripContentStream() {
        ResponseContent content = new ResponseContent();
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[] {1, 2, 3});
        content.setContent(stream);
        assertSame(stream, content.getContent());
    }

    /**
     * The header map accessor must round-trip a mutable map.
     */
    @Test
    void shouldRoundTripAllHeaders() {
        ResponseContent content = new ResponseContent();
        assertNull(content.getAllHeaders());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Request-Id", "abc");
        headers.put("Content-Type", "text/plain");
        content.setAllHeaders(headers);
        assertSame(headers, content.getAllHeaders());
        assertEquals("abc", content.getAllHeaders().get("X-Request-Id"));
    }

    /**
     * A freshly constructed instance must expose the documented default
     * field values (everything {@code null} or zero).
     */
    @Test
    void shouldInitialiseAllFieldsToDefault() {
        ResponseContent content = new ResponseContent();
        assertNull(content.getEncoding());
        assertNull(content.getContentBytes());
        assertEquals(0, content.getStatusCode());
        assertNull(content.getContentText());
        assertNull(content.getContentType());
        assertNull(content.getContentTypeString());
        assertNull(content.getContent());
        assertNull(content.getAllHeaders());
    }

    /**
     * The instance must remain non-null and independent of any other
     * instance created by the same constructor.
     */
    @Test
    void shouldProduceIndependentInstances() {
        ResponseContent a = new ResponseContent();
        ResponseContent b = new ResponseContent();
        assertNotNull(a);
        assertNotNull(b);
        a.setStatusCode(200);
        assertEquals(0, b.getStatusCode());
    }

    /**
     * The header map can be reassigned to a different map without affecting
     * the previously stored one.
     */
    @Test
    void shouldSupportReassigningHeaders() {
        ResponseContent content = new ResponseContent();
        Map<String, String> first = new HashMap<>();
        first.put("A", "1");
        content.setAllHeaders(first);

        Map<String, String> second = new HashMap<>();
        second.put("B", "2");
        content.setAllHeaders(second);

        assertSame(second, content.getAllHeaders());
        assertEquals(1, first.size());
    }
}
