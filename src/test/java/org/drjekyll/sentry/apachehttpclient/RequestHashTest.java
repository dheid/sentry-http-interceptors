package org.drjekyll.sentry.apachehttpclient;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RequestHashTest {

  @Mock
  private HttpUriRequest request;

  @Test
  void createsStableHash() {

    given(request.getMethod()).willReturn("GET");
    given(request.getURI()).willReturn(URI.create("https://www.daniel-heid.de/page?query=string"));

    int hash = RequestHash.create(request);

    assertThat(hash).isEqualTo(1039494016);

  }

}
