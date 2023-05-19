package org.drjekyll.sentry.apachehttpclient;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.hc.core5.util.Args;

import java.io.IOException;
import java.net.URISyntaxException;

import io.sentry.Breadcrumb;
import io.sentry.HubAdapter;
import io.sentry.IHub;
import io.sentry.ISpan;
import io.sentry.SpanStatus;

/**
 * An Apache HttpClient response interceptor that creates a Sentry breadcrumb and finishes the already created span by
 * {@link SentryHttpRequestInterceptor}.
 * <p>
 * Add this interceptor as last response interceptor with
 * {@link org.apache.http.impl.client.HttpClientBuilder#addInterceptorLast(HttpResponseInterceptor)} to measure the span
 * duration precisely as possible:
 * <pre>
 * HttpClientBuilder.create()
 *   .addInterceptorFirst(new SentryHttpRequestInterceptor(HubAdapter.getInstance()))
 *   .addInterceptorLast(new SentryHttpResponseInterceptor(HubAdapter.getInstance()))
 *   .build();
 * </pre>
 * This interceptor alone won't find the span, so you need {@link SentryHttpRequestInterceptor} as well. The hub needs
 * to be the same hub in both interceptors.
 */
public class SentryHttpResponseInterceptor implements HttpResponseInterceptor {

  private final IHub hub;

  /**
   * Initializes this HTTP response interceptor with the given Sentry hub.
   *
   * @param hub A Sentry hub, e.g. {@link HubAdapter#getInstance()}. Must not be null.
   */
  public SentryHttpResponseInterceptor(IHub hub) {
    this.hub = Args.notNull(hub, "Hub");
  }

  @Override
  public void process(HttpResponse response, EntityDetails entity, HttpContext context) throws IOException {
    Args.notNull(response, "HTTP response");
    Args.notNull(context, "HTTP context");
    ISpan span = hub.getSpan();
    if (span == null) {
      return;
    }
    Object requestAttribute = context.getAttribute(HttpCoreContext.HTTP_REQUEST);
    if (requestAttribute instanceof HttpRequest) {
      HttpRequest request = (HttpRequest) requestAttribute;
      try {
        if (isCorrespondingSpan(span, request)) {
          int statusCode = response.getCode();
          span.setStatus(SpanStatus.fromHttpStatusCode(statusCode));
          hub.addBreadcrumb(Breadcrumb.http(
            request.getUri().toString(),
            request.getMethod(),
            statusCode
          ));
          span.finish();
        }
      } catch (URISyntaxException e) {
        throw new IOException("Could not create request hash", e);
      }
    }
  }

  private static boolean isCorrespondingSpan(ISpan span, HttpRequest request) throws URISyntaxException {
    Args.notNull(span, "Span");
    Args.notNull(request, "HTTP Request");
    if (!"http.client".equals(span.getOperation())) {
      return false;
    }
    Object requestHashData = span.getData(RequestHash.SPAN_DATA_KEY);
    return requestHashData != null && requestHashData.equals(RequestHash.create(request));
  }

}
