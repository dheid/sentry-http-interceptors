# :flashlight: Sentry HTTP Interceptors

[![Maven Central](https://img.shields.io/maven-central/v/org.drjekyll/sentry-http-interceptors.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.drjekyll%22%20AND%20a:%22sentry-http-interceptors%22)
[![Java CI with Maven](https://github.com/dheid/sentry-http-interceptors/actions/workflows/build.yml/badge.svg)](https://github.com/dheid/sentry-http-interceptors/actions/workflows/build.yml)
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/W7W3EER56)

Sends tracing information about Apache HttpClient 5 calls to Sentry:

* Creates a http.client span that shows the HTTP method and the full target URL
* Adds a breadcrumb containing the HTTP URL, HTTP method and the HTTP response status code
* Includes Sentry trace and baggage headers to requests made with Apache HttpClient
* Easy to use
* Well documented with Javadoc

## :wrench: Usage

Include the dependency using Maven

```xml
<dependency>
  <groupId>org.drjekyll</groupId>
  <artifactId>sentry-http-interceptors</artifactId>
  <version>6.19.2</version>
</dependency>
```

or Gradle with Groovy DSL:

```groovy
implementation 'org.drjekyll:sentry-http-interceptors:6.19.2'
```

or Gradle with Kotlin DSL:

```kotlin
implementation("org.drjekyll:sentry-http-interceptors:6.19.2")
```

Run your build tool and add the interceptors like in the following example:

```java

CloseableHttpClient client = HttpClientBuilder.create()
  .addRequestInterceptorFirst(new SentryHttpRequestInterceptor(HubAdapter.getInstance()))
  .addResponseInterceptorLast(new SentryHttpResponseInterceptor(HubAdapter.getInstance()))
  .build();

```

After that the requests will be traced by Sentry, if Sentry is correctly configured in your application.

## :sunglasses: Development

To build and locally install the library and run the tests, just call

    mvn install

## :handshake: Contributing

Please read [the contribution document](CONTRIBUTING.md) for details on our code of conduct, and the
process for submitting pull requests to us.

## :notebook: Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see
the [tags on this repository](https://github.com/dheid/sentry-http-interceptors/tags).

## :scroll: License

This project is licensed under the LGPL License - see the [license](LICENSE) file for details.

## :loudspeaker: Release Notes

### 6.19.2

Using Apache HttpClient version 5 now

### > 6.9.0

Upgrade dependencies

### 6.9.0

Use Sentry version number to let consumer find the correct version more easily

### 1.0.2

Dependency updates

### 1.0.1

First public version
