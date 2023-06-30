package org.drjekyll.sentry.apachehttpclient5;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.junit.jupiter.api.Test;

import io.sentry.HubAdapter;
import io.sentry.ISpan;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
import io.sentry.SentryTraceHeader;
import io.sentry.SentryTracer;
import io.sentry.SpanStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class SentryApacheHttpClientIT {

  @Test
  void addsSpan(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {

    stubFor(get("/test").willReturn(ok()));

    SentryOptions options = new SentryOptions();
    options.setDsn("http://7caad69b389e41d98a74b1504b3c388f@localhost:" + wireMockRuntimeInfo.getHttpPort() + "/42");
    options.setTracesSampleRate(1.0);
    Sentry.init(options);
    SentryTracer transaction = (SentryTracer) Sentry.startTransaction(
      "TRANSACTION_NAME",
      "TRANSACTION_OPERATION",
      true
    );
    CloseableHttpClient client = HttpClientBuilder.create()
      .addRequestInterceptorFirst(new SentryHttpRequestInterceptor(HubAdapter.getInstance()))
      .addResponseInterceptorLast(new SentryHttpResponseInterceptor(HubAdapter.getInstance()))
      .build();

    HttpGet request = new HttpGet(wireMockRuntimeInfo.getHttpBaseUrl() + "/test");
    client.execute(request).close();
    transaction.finish();
    Sentry.flush(1000L);

    ISpan span = transaction.getChildren().get(0);
    assertThat(span.isFinished()).isTrue();
    String expectedDescription = String.format("GET %s/test", wireMockRuntimeInfo.getHttpBaseUrl());
    assertThat(span.getDescription()).isEqualTo(expectedDescription);
    assertThat(span.getOperation()).isEqualTo("http.client");
    assertThat(span.getStatus()).isEqualTo(SpanStatus.OK);
    assertThat(span.getData(RequestHash.SPAN_DATA_KEY)).isEqualTo(RequestHash.create(request));
    SentryTraceHeader sentryTraceHeader = span.toSentryTrace();
    verify(getRequestedFor(urlEqualTo("/test")).withHeader("sentry-trace", equalTo(sentryTraceHeader.getValue()))
      .withHeader(
        "baggage",
        equalTo("sentry-environment=production,sentry-public_key=7caad69b389e41d98a74b1504b3c388f,sentry-sample_rate=1,sentry-trace_id=" + sentryTraceHeader.getTraceId() + ",sentry-transaction=TRANSACTION_NAME")
      ));
    verify(postRequestedFor(urlEqualTo("/api/42/envelope/"))
      .withRequestBody(containing(sentryTraceHeader.getSpanId().toString()))
      .withRequestBody(containing("http.client"))
      .withRequestBody(containing(expectedDescription))
      .withRequestBody(containing("ok"))
    );

  }

}
