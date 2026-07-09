# httpclient-extension

Apache HttpClient 4.x 的纯 Java 扩展层，面向 `1.0.x` / JDK 8 版本线。

## 分层边界

- 本模块只承载非 Spring Boot 的 HttpClient 扩展能力，例如连接管理 builder、retry handler、请求/响应 interceptor、SSL 工具和通用 DTO。
- 本模块不使用 `spring-boot-starter-parent`，不包含 `@Configuration`、`@Conditional*`、`@ConfigurationProperties` 等 Boot 自动配置代码。
- `httpclient-spring-boot-starter` 只负责自动配置、属性绑定、Bean 装配和 Spring Qualifier。

## 版本线

- `httpclient-spring-boot-starter` 的 Boot 2.x 分支依赖 `httpclient-extension 1.0.x`。
- 后续如需要 Boot 3.x / 4.x 线，应分别使用 `httpclient-extension 2.0.x` / `3.0.x`，并按 JDK 17 / JDK 21 调整依赖栈。
