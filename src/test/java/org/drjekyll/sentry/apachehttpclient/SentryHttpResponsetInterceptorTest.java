package org.drjekyll.sentry.apachehttpclient;

import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
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

  @InjectMocks
  private SentryHttpResponseInterceptor sentryHttpResponseInterceptor;

  @Mock
  private IHub hub;

  @Mock
  private ISpan span;

  @Mock
  private HttpResponse response;

  @Mock
  private HttpRequest request;

  @Mock
  private HttpContext context;

  @Captor
  private ArgumentCaptor<Breadcrumb> breadcrumpCaptor;

  @Test
  void doesNothingIfNoSpanIsGiven() throws IOException {

    sentryHttpResponseInterceptor.process(response, null, context);

    verifyNoInteractions(span, response, context);

  }

  @Test
  void finishesSpan() throws Exception {

    given(request.getMethod()).willReturn(METHOD);
    given(request.getUri()).willReturn(URI.create(URL));
    given(context.getAttribute(HttpCoreContext.HTTP_REQUEST)).willReturn(request);
    given(hub.getSpan()).willReturn(span);
    given(span.getOperation()).willReturn("http.client");
    given(span.getData(RequestHash.SPAN_DATA_KEY)).willReturn(1039494016);
    given(response.getCode()).willReturn(STATUS_CODE);

    sentryHttpResponseInterceptor.process(response, null, context);

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
