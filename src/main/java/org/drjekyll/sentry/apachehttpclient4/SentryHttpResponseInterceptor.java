package org.drjekyll.sentry.apachehttpclient4;

import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.Args;

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
 * This interceptor alone won't find the span, so you need
 * {@link SentryHttpRequestInterceptor} as well. The hub needs to be the same hub in both interceptors.
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
  public void process(HttpResponse response, HttpContext context) {
    Args.notNull(response, "HTTP response");
    Args.notNull(context, "HTTP context");
    ISpan span = hub.getSpan();
    if (span == null) {
      return;
    }
    Object requestAttribute = context.getAttribute(HttpCoreContext.HTTP_REQUEST);
    if (requestAttribute instanceof HttpRequestWrapper) {
      HttpRequestWrapper request = (HttpRequestWrapper) requestAttribute;
      if (request.getOriginal() instanceof HttpUriRequest && isCorrespondingSpan(span, request)) {
        StatusLine statusLine = response.getStatusLine();
        if (statusLine != null) {
          int statusCode = statusLine.getStatusCode();
          span.setStatus(SpanStatus.fromHttpStatusCode(statusCode));
          HttpUriRequest original = (HttpUriRequest) request.getOriginal();
          hub.addBreadcrumb(Breadcrumb.http(
            original.getURI().toString(),
            original.getMethod(),
            statusCode
          ));
        }
        span.finish();
      }
    }
  }

  private static boolean isCorrespondingSpan(ISpan span, HttpUriRequest request) {
    Args.notNull(span, "Span");
    Args.notNull(request, "HTTP Request");
    if (!"http.client".equals(span.getOperation())) {
      return false;
    }
    Object requestHashData = span.getData(RequestHash.SPAN_DATA_KEY);
    return requestHashData != null && requestHashData.equals(RequestHash.create(request));
  }

}
