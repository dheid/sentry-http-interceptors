package org.drjekyll.sentry.apachehttpclient;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Args;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.sentry.BaggageHeader;
import io.sentry.Breadcrumb;
import io.sentry.HubAdapter;
import io.sentry.IHub;
import io.sentry.ISpan;
import io.sentry.SentryTraceHeader;
import io.sentry.util.PropagationTargetsUtils;

/**
 * An Apache HttpClient request interceptor that creates a Sentry span and adds Sentry tracing information to the HTTP
 * request headers.
 * <p>
 * Add this interceptor as first request interceptor with
 * {@link org.apache.http.impl.client.HttpClientBuilder#addInterceptorFirst(HttpRequestInterceptor)} to measure the span
 * duration precisely as possible:
 * <pre>
 * HttpClientBuilder.create()
 *   .addInterceptorFirst(new SentryHttpRequestInterceptor(HubAdapter.getInstance()))
 *   .addInterceptorLast(new SentryHttpResponseInterceptor(HubAdapter.getInstance()))
 *   .build();
 * </pre>
 * This interceptor alone won't finish the span and won't add a breadcrumb, so you need
 * {@link SentryHttpResponseInterceptor} as well.  The hub needs to be the same hub in both interceptors.
 */
public class SentryHttpRequestInterceptor implements HttpRequestInterceptor {

  private final IHub hub;

  /**
   * Initializes this HTTP request interceptor with the given Sentry hub.
   *
   * @param hub A Sentry hub, e.g. {@link HubAdapter#getInstance()}. Must not be null.
   */
  public SentryHttpRequestInterceptor(@Nonnull IHub hub) {
    this.hub = Args.notNull(hub, "Hub");
  }

  @Override
  public void process(@Nonnull HttpRequest request, @Nullable EntityDetails entity, @Nullable HttpContext context) throws IOException {
    Args.notNull(request, "HTTP request");
    ISpan activeSpan = hub.getSpan();
    String method = request.getMethod();
    String url;
    try {
      url = request.getUri().toString();
    } catch (URISyntaxException e) {
      throw new IOException("Could not read request URI", e);
    }
    if (activeSpan == null) {
      hub.addBreadcrumb(Breadcrumb.http(url, method));
      return;
    }
    ISpan childSpan = activeSpan.startChild("http.client");
    try {
      childSpan.setData(RequestHash.SPAN_DATA_KEY, RequestHash.create(request));
    } catch (URISyntaxException e) {
      throw new IOException("Could not create request hash", e);
    }
    childSpan.setDescription(String.format("%s %s", method, url));
    final SentryTraceHeader sentryTraceHeader = childSpan.toSentryTrace();
    if (PropagationTargetsUtils.contain(hub.getOptions().getTracePropagationTargets(), url)) {
      request.addHeader(sentryTraceHeader.getName(), sentryTraceHeader.getValue());
      Header[] headers = request.getHeaders(BaggageHeader.BAGGAGE_HEADER);
      if (headers != null) {
        BaggageHeader baggageHeader = childSpan.toBaggageHeader(Arrays.stream(headers)
          .sequential()
          .map(Header::getValue)
          .collect(Collectors.toList()));
        if (baggageHeader != null) {
          request.addHeader(baggageHeader.getName(), baggageHeader.getValue());
        }
      }
    }

  }

}

