package org.drjekyll.sentry.apachehttpclient;

import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicRequestLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import io.sentry.BaggageHeader;
import io.sentry.Breadcrumb;
import io.sentry.IHub;
import io.sentry.ISpan;
import io.sentry.SentryOptions;
import io.sentry.SentryTraceHeader;
import io.sentry.SpanId;
import io.sentry.protocol.SentryId;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SentryHttpRequestInterceptorTest {

  private static final String URL = "https://www.daniel-heid.de/page?query=string";

  private static final String METHOD = "GET";

  private static final String BAGGAGE = "sentry-environment=test,sentry-release=cc3bc71584bbf4765f90d6948749cc91b31c52ff,sentry-sample_rate=1,sentry-trace_id=984e8992456b4cb6b63c4bbb6be31d2b";

  private static final RequestLine REQUEST_LINE = new BasicRequestLine(METHOD, URL, new ProtocolVersion("HTTP", 1, 1));

  private static final BaggageHeader BAGGAGE_HEADER = new BaggageHeader(BAGGAGE);

  private static final SentryTraceHeader SENTRY_TRACE_HEADER = new SentryTraceHeader(new SentryId(), new SpanId(), null);

  @InjectMocks
  private SentryHttpRequestInterceptor sentryHttpRequestInterceptor;

  @Mock
  private IHub hub;

  @Mock
  private ISpan activeSpan;

  @Mock
  private ISpan childSpan;

  private HttpRequestWrapper requestWrapper;

  @Mock
  private HttpUriRequest originalRequest;

  @Mock
  private SentryOptions options;

  @Captor
  private ArgumentCaptor<Breadcrumb> breadcrumpCaptor;

  @Test
  void justAddsBreadcrumbIfNoSpanIsGiven() {

    givenRequest();

    sentryHttpRequestInterceptor.process(requestWrapper, null);

    verify(hub).addBreadcrumb(breadcrumpCaptor.capture());
    Breadcrumb breadcrumb = breadcrumpCaptor.getValue();
    assertThat(breadcrumb.getType()).isEqualTo("http");
    assertThat(breadcrumb.getCategory()).isEqualTo("http");
    assertThat(breadcrumb.getData("url")).isEqualTo(URL);
    assertThat(breadcrumb.getData("method")).isEqualTo(METHOD);

  }

  @Test
  void createsChildSpanAndAddsHeaders() {

    givenRequest();
    given(hub.getSpan()).willReturn(activeSpan);
    given(hub.getOptions()).willReturn(options);
    given(options.getTracePropagationTargets()).willReturn(singletonList(URL));
    given(activeSpan.startChild("http.client")).willReturn(childSpan);
    given(childSpan.toSentryTrace()).willReturn(SENTRY_TRACE_HEADER);
    requestWrapper.setHeader(BaggageHeader.BAGGAGE_HEADER, BAGGAGE);
    given(childSpan.toBaggageHeader(singletonList(BAGGAGE))).willReturn(BAGGAGE_HEADER);

    sentryHttpRequestInterceptor.process(requestWrapper, null);

    verify(childSpan).setData(RequestHash.SPAN_DATA_KEY, 1039494016);
    verify(childSpan).setDescription("GET https://www.daniel-heid.de/page?query=string");
    assertThat(requestWrapper.getFirstHeader(SentryTraceHeader.SENTRY_TRACE_HEADER).getValue()).isEqualTo(SENTRY_TRACE_HEADER.getValue());
    assertThat(requestWrapper.getFirstHeader(BaggageHeader.BAGGAGE_HEADER).getValue()).isEqualTo(BAGGAGE);

  }

  private void givenRequest() {
    given(originalRequest.getMethod()).willReturn(METHOD);
    given(originalRequest.getURI()).willReturn(URI.create(URL));
    given(originalRequest.getRequestLine()).willReturn(REQUEST_LINE);
    requestWrapper = HttpRequestWrapper.wrap(originalRequest);
  }

}
