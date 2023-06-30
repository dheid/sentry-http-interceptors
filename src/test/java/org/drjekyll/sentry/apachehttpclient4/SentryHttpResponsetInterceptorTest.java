package org.drjekyll.sentry.apachehttpclient4;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import io.sentry.Breadcrumb;
import io.sentry.IHub;
import io.sentry.ISpan;
import io.sentry.SpanStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SentryHttpResponsetInterceptorTest {

  private static final String URL = "https://www.daniel-heid.de/page?query=string";

  private static final String METHOD = "GET";

  private static final int STATUS_CODE = 200;

  private static final RequestLine REQUEST_LINE = new BasicRequestLine(METHOD, URL, new ProtocolVersion("HTTP", 1, 1));

  @InjectMocks
  private SentryHttpResponseInterceptor sentryHttpResponseInterceptor;

  @Mock
  private IHub hub;

  @Mock
  private ISpan span;

  @Mock
  private HttpResponse response;

  private HttpRequestWrapper requestWrapper;

  @Mock
  private HttpRequestWrapper originalRequest;

  @Mock
  private HttpContext context;

  @Mock
  private StatusLine statusLine;

  @Captor
  private ArgumentCaptor<Breadcrumb> breadcrumpCaptor;

  @Test
  void doesNothingIfNoSpanIsGiven() {

    sentryHttpResponseInterceptor.process(response, context);

    verifyNoInteractions(span, response, context, statusLine);

  }

  @Test
  void finishesSpan() {

    given(originalRequest.getMethod()).willReturn(METHOD);
    given(originalRequest.getURI()).willReturn(URI.create(URL));
    given(originalRequest.getRequestLine()).willReturn(REQUEST_LINE);
    requestWrapper = HttpRequestWrapper.wrap(originalRequest);
    given(context.getAttribute(HttpCoreContext.HTTP_REQUEST)).willReturn(requestWrapper);
    given(hub.getSpan()).willReturn(span);
    given(span.getOperation()).willReturn("http.client");
    given(span.getData(RequestHash.SPAN_DATA_KEY)).willReturn(1039494016);
    given(response.getStatusLine()).willReturn(statusLine);
    given(statusLine.getStatusCode()).willReturn(STATUS_CODE);

    sentryHttpResponseInterceptor.process(response, context);

    verify(span).setStatus(SpanStatus.OK);
    verify(span).finish();
    verify(hub).addBreadcrumb(breadcrumpCaptor.capture());
    Breadcrumb breadcrumb = breadcrumpCaptor.getValue();
    assertThat(breadcrumb.getType()).isEqualTo("http");
    assertThat(breadcrumb.getCategory()).isEqualTo("http");
    assertThat(breadcrumb.getData("url")).isEqualTo("https://www.daniel-heid.de/page");
    assertThat(breadcrumb.getData("http.query")).isEqualTo("query=string");
    assertThat(breadcrumb.getData("method")).isEqualTo(METHOD);
    assertThat(breadcrumb.getData("status_code")).isEqualTo(STATUS_CODE);


  }

}
