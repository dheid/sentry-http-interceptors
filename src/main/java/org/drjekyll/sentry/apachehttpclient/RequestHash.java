package org.drjekyll.sentry.apachehttpclient;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.Args;

final class RequestHash {

  static final String SPAN_DATA_KEY = "request.hash";

  private RequestHash() {
    // utility
  }

  static int create(HttpUriRequest httpUriRequest) {
    Args.notNull(httpUriRequest, "HTTP request");
    int uriHash = httpUriRequest.getURI() != null ? httpUriRequest.getURI().hashCode() : 0;
    return 31 * uriHash + (httpUriRequest.getMethod() != null ? httpUriRequest.getMethod().hashCode() : 0);
  }

}
