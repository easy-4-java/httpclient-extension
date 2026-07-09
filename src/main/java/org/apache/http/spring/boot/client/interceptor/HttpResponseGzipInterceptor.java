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

public class HttpResponseGzipInterceptor implements HttpResponseInterceptor {

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
