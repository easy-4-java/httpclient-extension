package org.apache.http.spring.boot.client.interceptor;

import java.io.IOException;
import java.util.Objects;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.protocol.HttpContext;

/**
 * {@link HttpResponseInterceptor} that transparently wraps
 * {@code gzip} and {@code deflate} encoded response entities with a
 * decompressing counterpart.
 *
 * <p>The interceptor inspects the {@code Content-Encoding} header of every
 * incoming response and, when it contains a {@code gzip} or {@code deflate}
 * entry, replaces the response entity with the corresponding
 * {@link GzipDecompressingEntity} or {@link DeflateDecompressingEntity}.
 * Other encodings (or a missing entity, or a missing
 * {@code Content-Encoding} header) are left untouched.</p>
 *
 * <p>The first matching codec wins &mdash; if a response advertises both
 * encodings, {@code gzip} is preferred because it is the first one checked.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see HttpResponseInterceptor
 * @see GzipDecompressingEntity
 * @see DeflateDecompressingEntity
 */
public class HttpResponseGzipInterceptor implements HttpResponseInterceptor {

	/**
	 * Walks the response entity's {@code Content-Encoding} header and wraps
	 * the entity in a decompressing counterpart when applicable.
	 *
	 * @param response the current response.
	 * @param context the current execution context (unused).
	 * @throws HttpException never thrown by this implementation; declared for compatibility.
	 * @throws IOException never thrown by this implementation; declared for compatibility.
	 */
	@Override
	public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
		HttpEntity entity = response.getEntity();
		if (Objects.isNull(entity)) {
			return;
		}
		Header ceheader = entity.getContentEncoding();
		if (Objects.isNull(ceheader)) {
			return;
		}
		HeaderElement[] codecs = ceheader.getElements();
		for (HeaderElement codec : codecs) {
			if ("gzip".equalsIgnoreCase(codec.getName())) {
				response.setEntity(new GzipDecompressingEntity(response.getEntity()));
				return;
			}
			if ("deflate".equalsIgnoreCase(codec.getName())) {
				response.setEntity(new DeflateDecompressingEntity(response.getEntity()));
				return;
			}
		}
	}
}
