package org.apache.http.spring.boot.client.utils;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.config.MessageConstraints;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.conn.DefaultHttpResponseParser;
import org.apache.http.impl.conn.DefaultHttpResponseParserFactory;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.util.CharArrayBuffer;

/**
 * Factory for the lenient {@link HttpMessageParserFactory} and
 * {@link HttpMessageWriterFactory} implementations used by easy-4-java.
 *
 * <p>The default response parser used by Apache HttpClient 4.x is strict:
 * malformed headers raise a {@link ParseException} and the {@code reject}
 * hook can be used to drop responses that look suspicious. The factories
 * returned by this class relax both behaviours so that proxies or servers
 * that occasionally emit non-conforming wire data (e.g. headers without a
 * colon, custom status lines) are still consumed gracefully.</p>
 *
 * <p>This class is not instantiable.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see HttpMessageParserFactory
 * @see HttpMessageWriterFactory
 */
public final class HttpMessageFactoryUtils {

	/**
	 * Lenient response parser factory: catches {@link ParseException} from
	 * header parsing and returns a {@link BasicHeader} with a {@code null}
	 * value, and disables the {@code reject} hook so that no response is
	 * discarded on the wire.
	 */
	private static final HttpMessageParserFactory<HttpResponse> RESPONSE_PARSER_FACTORY =
			new DefaultHttpResponseParserFactory() {
				@Override
				public HttpMessageParser<HttpResponse> create(SessionInputBuffer buffer,
						MessageConstraints constraints) {
					LineParser lineParser = new BasicLineParser() {
						@Override
						public Header parseHeader(CharArrayBuffer buffer) {
							try {
								return super.parseHeader(buffer);
							} catch (ParseException ex) {
								return new BasicHeader(buffer.toString(), null);
							}
						}
					};
					return new DefaultHttpResponseParser(buffer, lineParser,
							DefaultHttpResponseFactory.INSTANCE, constraints) {
						@Override
						protected boolean reject(CharArrayBuffer line, int count) {
							return false;
						}
					};
				}
			};

	/** Standard request writer factory; equivalent to {@link DefaultHttpRequestWriterFactory}. */
	private static final HttpMessageWriterFactory<HttpRequest> REQUEST_WRITER_FACTORY =
			new DefaultHttpRequestWriterFactory();

	/**
	 * Private constructor &mdash; the class is a static utility holder.
	 */
	private HttpMessageFactoryUtils() {
	}

	/**
	 * Returns the shared lenient response parser factory.
	 *
	 * @return a singleton {@link HttpMessageParserFactory} for {@link HttpResponse}; never {@code null}.
	 */
	public static HttpMessageParserFactory<HttpResponse> getResponseParserFactory() {
		return RESPONSE_PARSER_FACTORY;
	}

	/**
	 * Returns the shared request writer factory.
	 *
	 * @return a singleton {@link HttpMessageWriterFactory} for {@link HttpRequest}; never {@code null}.
	 */
	public static HttpMessageWriterFactory<HttpRequest> getRequestWriterFactory() {
		return REQUEST_WRITER_FACTORY;
	}
}
