package org.drjekyll.sentry.apachehttpclient5;

import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RequestHashTest {

  @Mock
  private HttpRequest request;

  @Test
  void createsStableHash() throws URISyntaxException {

    given(request.getMethod()).willReturn("GET");
    given(request.getUri()).willReturn(URI.create("https://www.daniel-heid.de/page?query=string"));

    int hash = RequestHash.create(request);

    assertThat(hash).isEqualTo(1039494016);

  }

}
