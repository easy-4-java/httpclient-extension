# httpclient-extension

[English](./README.md) | [简体中文](./README.zh-CN.md)

Pure Java extensions for Apache HttpClient 4.x — connection manager builder, retry handler, request/response interceptors, SSL utilities and common DTOs. No Spring Boot auto-configuration in this module.

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage / API](#8-core-usage--api)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

`httpclient-extension` (project description: *Pure Java extensions for Apache HttpClient 4.x*) is the non-Spring-Boot extension layer for Apache HttpClient 4.x. It carries connection-management builders, retry handlers, request/response interceptors, SSL utilities and common DTOs, and is consumed by the companion `httpclient-spring-boot-starter` (which is responsible for auto-configuration, property binding and bean assembly).

| What it is | What it is not |
|:---|:---|
| A pure Java extension layer for HttpClient 4.x | A Spring Boot starter (no `@Configuration`, `@Conditional*` or `@ConfigurationProperties` code) |
| Connection manager builder, retry handler, interceptors, SSL utils, DTOs | A replacement for HttpClient itself |
| Spring-independent, directly usable in any Java app | A web framework |

Typical use cases:

| Use case | Notes |
|:---|:---|
| Build a `PoolingHttpClientConnectionManager` | `HttpClientConnectionManagerBuilder` (max totals, per-route limits, TTL, DNS resolver, socket config) |
| Retry failed requests | `HttpRequestExceptionRetryHandler` (retry count, request-sent-retry policy) |
| Add default headers / gzip support | `HttpRequestHeaderInterceptor`, `HttpRequestGzipInterceptor`, `HttpResponseGzipInterceptor` |
| Request summary logging | `HttpRequestSummaryInterceptor` |
| TLS setup | `SSLContextUtils` / `TrustManagerUtils` / `TrustStrategyUtils` (self-signed, accept-all, validate) |
| Carry HTTP response data around | `ResponseContent` DTO, `HttpClientParams` constants |

**Project status:** stable.

## 2. Features & Status

| Feature | Status | Notes |
|:---|:---|:---|
| `HttpClientConnectionManagerBuilder` | Available | `create()` -> setters -> `build()` returns `PoolingHttpClientConnectionManager` |
| `HttpRequestExceptionRetryHandler` | Available | Implements `HttpRequestRetryHandler`; `retryRequest(IOException, executionCount, context)` |
| `HttpRequestHeaderInterceptor` | Available | Header injection from `Properties` or `Map<String,String>` |
| `HttpRequestGzipInterceptor` / `HttpResponseGzipInterceptor` | Available | Gzip request/response handling |
| `HttpRequestSummaryInterceptor` | Available | Request summary logging (implements `HttpRequestInterceptor`) |
| `SSLContextUtils` | Available | SSL contexts from protocols, keystores or defaults |
| `TrustManagerUtils` / `TrustStrategyUtils` | Available | Trust managers and strategies (self-signed / accept-all / validate) |
| `HttpMessageFactoryUtils` | Available | Message factory helpers |
| `ResponseContent` / `HttpClientParams` | Available | Response DTO (`encoding`, `contentBytes`, `statusCode`, `contentText`, `contentType`) and parameter name constants |
| Unit tests | Not present | No test sources in the repository |
| CI pipeline | Not configured | No CI workflow files in the repository |

## 3. Requirements & Compatibility

| Requirement | Version |
|:---|:---|
| JDK | 8 |
| Maven | 3.0+ |
| Apache HttpClient | 4.x (`org.apache.httpcomponents:httpclient`, `httpcore`) |
| slf4j-api | 2.0.x (declared) |
| Lombok | provided scope (annotation processing at build time) |

### Version lines

| Branch | JDK | Version pattern |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

## 4. Architecture & Modules

```text
  Your code / httpclient-spring-boot-starter
                     |
                     v
        httpclient-extension (pure Java, no Boot)
   ----------------------------------------------------
   HttpClientConnectionManagerBuilder  ->  PoolingHttpClientConnectionManager
   HttpRequestExceptionRetryHandler    ->  retry policy
   HttpRequest/Response*Interceptor    ->  headers, gzip, summary
   SSLContextUtils / TrustManagerUtils ->  TLS setup
   ResponseContent / HttpClientParams ->  DTO + constants
   ----------------------------------------------------
                     |
                     v
        Apache HttpClient 4.x (httpclient + httpcore)
```

Single module, jar packaging (base package `org.apache.http.spring.boot.client`):

| Sub-package | Responsibility |
|:---|:---|
| `org.apache.http.spring.boot.client` | `HttpClientConnectionManagerBuilder`, `HttpClientParams`, `ResponseContent` |
| `...client.handler` | `HttpRequestExceptionRetryHandler` |
| `...client.interceptor` | `HttpRequestSummaryInterceptor`, `HttpRequestGzipInterceptor`, `HttpResponseGzipInterceptor`, `HttpRequestHeaderInterceptor` |
| `...client.utils` | `SSLContextUtils`, `TrustManagerUtils`, `TrustStrategyUtils`, `HttpMessageFactoryUtils` |

## 5. Installation

### Maven

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>httpclient-extension</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.easy4j:httpclient-extension:1.0.x.20260630-SNAPSHOT'
```

**Availability:** the artifact is published to the Aliyun private Maven repository and distributed through GitHub Releases; it has not yet been published to Maven Central.

## 6. Quick Start

```java
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.spring.boot.client.HttpClientConnectionManagerBuilder;
import org.apache.http.spring.boot.client.handler.HttpRequestExceptionRetryHandler;
import org.apache.http.spring.boot.client.interceptor.HttpRequestHeaderInterceptor;

import java.util.HashMap;
import java.util.Map;

PoolingHttpClientConnectionManager cm = HttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(200)
        .setMaxConnPerRoute(50)
        .build();

Map<String, String> headers = new HashMap<>();
headers.put("X-Client", "httpclient-extension");

CloseableHttpClient client = HttpClients.custom()
        .setConnectionManager(cm)
        .addInterceptorFirst(new HttpRequestHeaderInterceptor(headers))
        .setRetryHandler(new HttpRequestExceptionRetryHandler(3))
        .build();
```

Expected result: a configured `CloseableHttpClient` with pooled connections, default headers and up to 3 retries per failed request.

## 7. Configuration

The module is a plain library: no configuration files, no property prefixes. All settings are passed programmatically (builder setters, constructor arguments such as `retryTime` / `requestSentRetryEnabled`, header maps, trust strategies).

## 8. Core Usage / API

### 8.1 Connection manager builder

| Method | Description |
|:---|:---|
| `create()` | New builder instance |
| `setMaxConnTotal(int)` / `setMaxConnPerRoute(int)` | Pool limits |
| `setDefaultConnectionConfig(ConnectionConfig)` / `setDefaultSocketConfig(SocketConfig)` | Default configs |
| `setDnsResolver(DnsResolver)` | DNS resolution |
| `setConnectionTimeToLive(long, TimeUnit)` | Connection TTL |
| `setPublicSuffixMatcher(PublicSuffixMatcher)` | Public suffix matching |
| `build()` | `PoolingHttpClientConnectionManager` |

### 8.2 SSL trust strategies

```java
import org.apache.http.spring.boot.client.utils.SSLContextUtils;
import org.apache.http.spring.boot.client.utils.TrustStrategyUtils;

// Accept-all trust strategy (dev/test only)
SSLContext context = SSLContextUtils.createSSLContext(
        null, TrustStrategyUtils.getAcceptAllTrustStrategy());
```

`TrustStrategyUtils` also provides `getSelfSignedTrustStrategy()` (self-signed certificates) and `getValidateCertificateTrustStrategy()` (strict validation); `SSLContextUtils.createDefaultSSLContext()` returns a default context.

### 8.3 Retry handler

```java
HttpRequestExceptionRetryHandler retry = new HttpRequestExceptionRetryHandler(3, true);
```

## 9. Testing & Build

```bash
./mvnw clean verify        # compile, run tests, generate coverage report
./mvnw clean install       # install into the local repository
```

- The repository currently contains no test sources.
- Coverage is measured with the JaCoCo Maven plugin (target: 90% line coverage, `haltOnFailure=false`).
- The `release` profile assembles GPG signing + sources + Javadoc + deployment (`./mvnw -Prelease clean deploy`).

## 10. Versioning & Branches

Three parallel version lines are maintained:

| Branch | JDK | Version pattern |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

Maintenance strategy: the 1.0.x line serves Boot 2.x consumers of the companion starter; the 2.0.x / 3.0.x lines serve Boot 3.x / 4.x generations with JDK 17 / 21 dependency stacks.

## 11. Contributing & License

Contributions are welcome — open an issue or submit a pull request against the matching version-line branch (`feature/1.0.x` for JDK 8 changes).

This project is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0). See the `LICENSE` file in the repository root for details.
