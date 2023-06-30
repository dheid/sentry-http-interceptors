package org.drjekyll.sentry.apachehttpclient5;

import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.util.Args;

import java.net.URISyntaxException;

final class RequestHash {

  static final String SPAN_DATA_KEY = "request.hash";

  private RequestHash() {
    // utility
  }

  static int create(HttpRequest httpRequest) throws URISyntaxException {
    Args.notNull(httpRequest, "HTTP request");
    int uriHash = httpRequest.getUri() != null ? httpRequest.getUri().hashCode() : 0;
    return 31 * uriHash + (httpRequest.getMethod() != null ? httpRequest.getMethod().hashCode() : 0);
  }

}
