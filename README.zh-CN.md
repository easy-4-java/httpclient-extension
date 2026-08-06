# httpclient-extension

[English](./README.md) | [简体中文](./README.zh-CN.md)

Apache HttpClient 4.x 的纯 Java 扩展层 — 连接管理器 builder、重试处理器、请求/响应拦截器、SSL 工具与通用 DTO。本模块不含 Spring Boot 自动配置。

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 功能与状态](#2-features--status)
- [3. 环境要求与兼容性](#3-requirements--compatibility)
- [4. 架构与模块](#4-architecture--modules)
- [5. 安装](#5-installation)
- [6. 快速开始](#6-quick-start)
- [7. 配置](#7-configuration)
- [8. 核心用法 / API](#8-core-usage--api)
- [9. 测试与构建](#9-testing--build)
- [10. 版本线与分支](#10-versioning--branches)
- [11. 参与贡献与许可协议](#11-contributing--license)

## 1. 项目概览

`httpclient-extension`（项目描述：*Pure Java extensions for Apache HttpClient 4.x*）是 Apache HttpClient 4.x 的非 Spring Boot 扩展层。它承载连接管理 builder、重试处理器、请求/响应拦截器、SSL 工具与通用 DTO，供配套的 `httpclient-spring-boot-starter`（负责自动配置、属性绑定与 Bean 装配）消费。

| 是什么 | 不是什么 |
|:---|:---|
| HttpClient 4.x 的纯 Java 扩展层 | Spring Boot Starter（不含 `@Configuration`、`@Conditional*`、`@ConfigurationProperties` 代码） |
| 连接管理器 builder、重试处理器、拦截器、SSL 工具、DTO | HttpClient 本身的替代品 |
| 无 Spring 依赖，可直接用于任意 Java 应用 | Web 框架 |

典型使用场景：

| 场景 | 说明 |
|:---|:---|
| 构建 `PoolingHttpClientConnectionManager` | `HttpClientConnectionManagerBuilder`（总连接数、每路由上限、TTL、DNS 解析、socket 配置） |
| 请求失败重试 | `HttpRequestExceptionRetryHandler`（重试次数、已发送请求重试策略） |
| 默认请求头 / gzip 支持 | `HttpRequestHeaderInterceptor`、`HttpRequestGzipInterceptor`、`HttpResponseGzipInterceptor` |
| 请求摘要日志 | `HttpRequestSummaryInterceptor` |
| TLS 配置 | `SSLContextUtils` / `TrustManagerUtils` / `TrustStrategyUtils`（自签名、全信任、严格校验） |
| 携带 HTTP 响应数据 | `ResponseContent` DTO、`HttpClientParams` 常量 |

**项目状态：** 稳定。

<a id="2-features--status"></a>
## 2. 功能与状态

| 能力 | 状态 | 说明 |
|:---|:---|:---|
| `HttpClientConnectionManagerBuilder` | 可用 | `create()` -> 各 setter -> `build()` 返回 `PoolingHttpClientConnectionManager` |
| `HttpRequestExceptionRetryHandler` | 可用 | 实现 `HttpRequestRetryHandler`；`retryRequest(IOException, executionCount, context)` |
| `HttpRequestHeaderInterceptor` | 可用 | 从 `Properties` 或 `Map<String,String>` 注入请求头 |
| `HttpRequestGzipInterceptor` / `HttpResponseGzipInterceptor` | 可用 | 请求/响应 gzip 处理 |
| `HttpRequestSummaryInterceptor` | 可用 | 请求摘要日志（实现 `HttpRequestInterceptor`） |
| `SSLContextUtils` | 可用 | 从协议、密钥库或默认值创建 SSL 上下文 |
| `TrustManagerUtils` / `TrustStrategyUtils` | 可用 | 信任管理器与信任策略（自签名 / 全信任 / 严格校验） |
| `HttpMessageFactoryUtils` | 可用 | 消息工厂辅助 |
| `ResponseContent` / `HttpClientParams` | 可用 | 响应 DTO（`encoding`、`contentBytes`、`statusCode`、`contentText`、`contentType`）与参数名常量 |
| 单元测试 | 无 | 仓库中无测试源码 |
| CI 流水线 | 未配置 | 仓库中无 CI 工作流文件 |

<a id="3-requirements--compatibility"></a>
## 3. 环境要求与兼容性

| 依赖项 | 版本 |
|:---|:---|
| JDK | 8 |
| Maven | 3.0+ |
| Apache HttpClient | 4.x（`org.apache.httpcomponents:httpclient`、`httpcore`） |
| slf4j-api | 2.0.x（已声明） |
| Lombok | provided 作用域（构建期注解处理） |

### 版本线矩阵

| 分支 | JDK | 版本号模式 |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

<a id="4-architecture--modules"></a>
## 4. 架构与模块

```text
  业务代码 / httpclient-spring-boot-starter
                     |
                     v
        httpclient-extension（纯 Java，无 Boot）
   ----------------------------------------------------
   HttpClientConnectionManagerBuilder  ->  PoolingHttpClientConnectionManager
   HttpRequestExceptionRetryHandler    ->  重试策略
   HttpRequest/Response*Interceptor    ->  请求头、gzip、摘要
   SSLContextUtils / TrustManagerUtils ->  TLS 配置
   ResponseContent / HttpClientParams ->  DTO + 常量
   ----------------------------------------------------
                     |
                     v
        Apache HttpClient 4.x（httpclient + httpcore）
```

单一模块，jar 打包（基础包 `org.apache.http.spring.boot.client`）：

| 子包 | 职责 |
|:---|:---|
| `org.apache.http.spring.boot.client` | `HttpClientConnectionManagerBuilder`、`HttpClientParams`、`ResponseContent` |
| `...client.handler` | `HttpRequestExceptionRetryHandler` |
| `...client.interceptor` | `HttpRequestSummaryInterceptor`、`HttpRequestGzipInterceptor`、`HttpResponseGzipInterceptor`、`HttpRequestHeaderInterceptor` |
| `...client.utils` | `SSLContextUtils`、`TrustManagerUtils`、`TrustStrategyUtils`、`HttpMessageFactoryUtils` |

<a id="5-installation"></a>
## 5. 安装

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

**可用性：** 构件发布至阿里云私有 Maven 仓库，并通过 GitHub Releases 分发；尚未发布到 Maven Central。

<a id="6-quick-start"></a>
## 6. 快速开始

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

预期结果：得到配置完成的 `CloseableHttpClient` — 连接池化、带默认请求头、失败请求最多重试 3 次。

<a id="7-configuration"></a>
## 7. 配置

本模块为纯库：无配置文件、无属性前缀。全部设置以编程方式传入（builder setter、构造参数如 `retryTime` / `requestSentRetryEnabled`、请求头 Map、信任策略等）。

<a id="8-core-usage--api"></a>
## 8. 核心用法 / API

### 8.1 连接管理器 builder

| 方法 | 说明 |
|:---|:---|
| `create()` | 创建 builder 实例 |
| `setMaxConnTotal(int)` / `setMaxConnPerRoute(int)` | 连接池上限 |
| `setDefaultConnectionConfig(ConnectionConfig)` / `setDefaultSocketConfig(SocketConfig)` | 默认配置 |
| `setDnsResolver(DnsResolver)` | DNS 解析 |
| `setConnectionTimeToLive(long, TimeUnit)` | 连接 TTL |
| `setPublicSuffixMatcher(PublicSuffixMatcher)` | 公共后缀匹配 |
| `build()` | 返回 `PoolingHttpClientConnectionManager` |

### 8.2 SSL 信任策略

```java
import org.apache.http.spring.boot.client.utils.SSLContextUtils;
import org.apache.http.spring.boot.client.utils.TrustStrategyUtils;

// 全信任策略（仅限开发/测试）
SSLContext context = SSLContextUtils.createSSLContext(
        null, TrustStrategyUtils.getAcceptAllTrustStrategy());
```

`TrustStrategyUtils` 还提供 `getSelfSignedTrustStrategy()`（自签名证书）与 `getValidateCertificateTrustStrategy()`（严格校验）；`SSLContextUtils.createDefaultSSLContext()` 返回默认上下文。

### 8.3 重试处理器

```java
HttpRequestExceptionRetryHandler retry = new HttpRequestExceptionRetryHandler(3, true);
```

<a id="9-testing--build"></a>
## 9. 测试与构建

```bash
./mvnw clean verify        # 编译、运行测试、生成覆盖率报告
./mvnw clean install       # 安装到本地仓库
```

- 仓库当前不含测试源码。
- 覆盖率由 JaCoCo Maven 插件度量（目标：90% 行覆盖率，`haltOnFailure=false`）。
- `release` profile 组装 GPG 签名 + 源码 + Javadoc + 部署（`./mvnw -Prelease clean deploy`）。

<a id="10-versioning--branches"></a>
## 10. 版本线与分支

仓库维护三条并行版本线：

| 分支 | JDK | 版本号模式 |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

维护策略：1.0.x 版本线服务于配套 starter 的 Boot 2.x 消费方；2.0.x / 3.0.x 版本线面向 Boot 3.x / 4.x 一代，使用 JDK 17 / JDK 21 依赖栈。

<a id="11-contributing--license"></a>
## 11. 参与贡献与许可协议

欢迎参与贡献——请通过 Issue 反馈问题，或向对应版本线分支提交 Pull Request（JDK 8 相关改动提交到 `feature/1.0.x`）。

本项目基于 [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) 许可发布。详见仓库根目录的 `LICENSE` 文件。
