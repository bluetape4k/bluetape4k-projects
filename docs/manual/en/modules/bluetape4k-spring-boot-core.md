---
manualId: bluetape4k-spring-boot-core
title: "Spring Boot Application Foundations"
description: "Kotlin-friendly adapters for Spring Context, HTTP, WebFlux, error responses, observations, and execution resources."
kind: library
group: spring
learningOrder: 900
---

# Spring Boot Application Foundations

## What it provides {#problem}

`bluetape4k-spring-boot-core` collects small adapters that otherwise recur across Spring Framework and Spring Boot applications. It provides bean and merged-annotation lookup, profile and property access, coroutine calls for `RestClient`, WebFlux request context and `DataBuffer` helpers, standard error responses, Micrometer Observation helpers, and dedicated `WebClient` resource configuration.

It does not replace Spring Boot. Spring Boot and the application still configure the application context, HTTP client implementation, JSON converters, Actuator exporters, security, and transactions. This module adds Kotlin extensions and explicitly imported configuration on top of those facilities.

## Decide before adopting it {#when-to-use}

- Separate Spring Context helpers from HTTP and WebFlux helpers. Do not treat the whole artifact as an application framework.
- Adding `VirtualThreadAutoConfiguration` to the classpath does not enable it. Import it explicitly with `@Import`.
- `HttpRequestCapturer` and `ApiExceptionHandler` must also be inside the application's component-scan range.
- `RestClient` is blocking. The suspend extensions run blocking calls interruptibly on `Dispatchers.IO`; they do not turn the client into reactive I/O.
- A dedicated WebClient event loop or controller coroutine scope introduces an ownership and shutdown obligation.

If you only need Spring Data R2DBC coroutine extensions, start with [`bluetape4k-spring-boot-r2dbc`](./bluetape4k-spring-boot-r2dbc.md). For JDBC and transaction helpers, see [`bluetape4k-jdbc`](./bluetape4k-jdbc.md).

## Add the dependency {#coordinates}

Consumers manage only the `bluetape4k-dependencies` BOM version. Add the Spring starter that supplies the capability used by the application.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-core")

    implementation("org.springframework.boot:spring-boot-starter-webflux") // choose for the APIs you use
}
```

Most Web, WebFlux, Spring Data, Micrometer, and Netty dependencies of this module are `compileOnly`. The matching starter or library must be present at runtime before its APIs can be used.

## First examples {#quick-start}

Use reified extensions for Spring merged-annotation lookup:

```kotlin
val mapping = handlerMethod.findMergedAnnotationOrNull<RequestMapping>()
val isMapped = handlerMethod.hasMergedAnnotation<RequestMapping>()
```

Call a blocking `RestClient` from a coroutine service like this:

```kotlin
class UserClient(
    private val client: RestClient,
) {
    suspend fun find(id: Long): User =
        client.suspendGet("/users/$id", MediaType.APPLICATION_JSON)
}
```

`suspendGet` uses `runInterruptible(Dispatchers.IO)`. Coroutine cancellation can interrupt the client thread, but whether that aborts the request depends on the `ClientHttpRequestFactory`.

## API by task {#api-by-task}

| Task | API | Boundary to keep visible |
| --- | --- | --- |
| Find merged annotations | `findMergedAnnotationOrNull`, `hasMergedAnnotation` | Preserves Spring meta-annotation and `@AliasFor` semantics. |
| Look up beans | `BeanFactory.get`, `findOrNull` family | Missing beans may become `null`; ambiguous beans remain errors. |
| Declare profiles | `LocalProfile`, `DevelopProfile`, `ProductionProfile`, and peers | Aliases Spring `@Profile`; does not create a property source. |
| Read properties | `PropertyResolver.getAs`, `getRequiredPropertyAs` | Uses Spring conversion; a missing required value fails. |
| Call blocking HTTP | `restClientOf`, `httpGet`, `suspendGet` family | The application owns request factory, converters, and timeouts. |
| Read request context | `HttpRequestCapturer`, `HttpRequestHolder` | The current request exists only inside Reactor Context. |
| Stream buffers | `readAsDataBuffer`, `write`, `join`, `release` | Distinguish cold flows, byte limits, and pooled-buffer release. |
| Return API errors | `ApiExceptionHandler`, `ApiErrorBody` | Handles a fixed exception set and exposes exception messages. |
| Observe code | `observeSpring`, `observeSpringSuspending` | The application supplies the registry and exporter. |
| Configure a dedicated WebClient | `AbstractWebClientConfig` | Owns event loop, SSL, timeout, codec memory, and shutdown together. |
| Create a virtual-thread executor | `VirtualThreadAutoConfiguration` | Requires `@Import` and backs off when an `AsyncTaskExecutor` exists. |

## Learning path {#concepts}

Each chapter is grounded in the 1.12.1 source and tests. The chapters explain configuration ownership, lifecycle, and failure propagation instead of stopping at an API inventory.

1. [Spring Context and configuration helpers](./bluetape4k-spring-boot-core/spring-context-and-configuration.md) — annotation, bean, profile, and property helpers, including what is not auto-configured.
2. [RestClient and coroutine boundaries](./bluetape4k-spring-boot-core/rest-client-and-coroutines.md) — blocking HTTP, nullable bodies, cancellation, and converter ownership.
3. [WebFlux request context and DataBuffer](./bluetape4k-spring-boot-core/webflux-request-and-buffers.md) — Reactor Context, internal path rewriting, cold flows, and pooled-buffer lifecycle.
4. [Error responses and observations](./bluetape4k-spring-boot-core/error-responses-and-observability.md) — status mapping, message exposure, Observation scopes, and cardinality.
5. [Execution resources and lifecycle](./bluetape4k-spring-boot-core/execution-resources-and-lifecycle.md) — dedicated WebClient loops, coroutine scopes, and virtual-thread executors.
6. [Testing and ecosystem paths](./bluetape4k-spring-boot-core/testing-and-ecosystem-paths.md) — focused verification and the next Spring Data, WebFlux, and observability modules.

Start with chapter 1, then jump to the capability you need. For HTTP clients, read 2 then 5. For WebFlux server code, read 3, 4, and 5.

## Recommended patterns {#patterns}

Let application services and configuration retain ownership of Spring objects, then import only the extensions needed at a call site. Do not let `BeanFactory.findOrNull` hide ambiguous beans, and use `getRequiredPropertyAs` when a missing value should fail startup.

Choose blocking `RestClient` or reactive `WebClient` according to the whole call path. Wrapping `RestClient` in a suspend function does not make it a non-blocking event-loop client. Conversely, a dedicated WebClient loop should keep thread count, timeouts, SSL trust, and shutdown in one configuration.

## Integration boundaries {#integrations}

```text
Spring Boot application configuration
        ├── Spring Context / PropertyResolver
        ├── RestClient + request factory + message converters
        ├── WebFlux + Reactor Context + DataBuffer
        ├── ObservationRegistry + application exporter
        └── application-owned executors and lifecycle
                         ↓
             bluetape4k Spring helper APIs
```

`bluetape4k-spring-boot-core` is not a general-purpose starter that creates these objects. The exceptions are the bean factory methods in `AbstractWebClientConfig` and an explicitly imported `VirtualThreadAutoConfiguration`.

## Configuration {#configuration}

The module directly reads one property: `bluetape4k.webclient.response-timeout` for `AbstractWebClientConfig.responseTimeout`. Its default is the ISO-8601 duration `PT30S`; Spring conversion also accepts values such as `30s`.

```yaml
bluetape4k:
  webclient:
    response-timeout: 5s
```

There is no dedicated configuration-properties class or `META-INF/spring/...AutoConfiguration.imports`. Override `threadCount`, connect timeout, shutdown timeout, codec memory, and SSL context by subclassing `AbstractWebClientConfig`. The profile annotations are `@Profile` meta-annotations, not separate configuration stores.

## Failure behavior {#failures}

`getRequiredPropertyAs` propagates Spring property errors when a value is missing or cannot be converted. `BeanFactory.findOrNull` changes only bean absence into `null`; it rethrows `NoUniqueBeanDefinitionException`.

HTTP status and decoding errors from `RestClient.retrieve()` propagate unchanged. Non-null functions such as `suspendGet<T>` fail if the response body is absent; use `suspendGetOrNull<T>` when an empty body is valid.

`ApiExceptionHandler` covers only the declared API exceptions and `HttpMessageNotReadableException`. Its body omits stack traces but includes `exception.message`, so domain exceptions must contain messages safe for external clients.

## Operations {#operations}

When using `AbstractWebClientConfig`, monitor the dedicated Netty loop's thread count, connect and response timeouts, codec memory limit, and shutdown time. The default SSL context validates certificates against the JDK trust store. Never use `insecureSslContext()` outside development or tests.

Observation helpers start an observation, open its scope, and always stop it. Cancellation is rethrown without being recorded as an error. Keep metrics labels bounded and low-cardinality; reserve request IDs and similar values for high-cardinality trace fields.

## Testing {#testing}

```bash
./gradlew :bluetape4k-spring-boot-core:test --no-configuration-cache
```

`CustomWebClientConfigTest` verifies dedicated Reactor resources and the WebClient bean. `RestClientCoroutinesDslTest` covers success, nullable bodies, and cancellation interrupts. `HttpRequestFilterTest` checks request propagation through Reactor Context, while `SpringObservationSupportTest` checks start/error/stop and cancellation cleanup.

`ApiExceptionHandlerTest` and `DataBufferSupportTest` cover status codes, bodies, byte limits, reads and writes, and pooled-buffer release.

## Workshops and examples {#workshops}

No dedicated workshop is registered in the manual manifest. The README HTTP and observation examples plus the module's test fixtures are the runnable learning material. `CustomWebClientConfig`, `WebClientReadmeExamplesTest`, and `SpringObservationSupportTest` form a useful path from configuration to a call site.

For data access, continue with the Spring Data JDBC or R2DBC manuals. For lower-level coroutine and Flow rules, see [`bluetape4k-coroutines`](./bluetape4k-coroutines.md).

## 1.12.1 scope {#limitations}

This manual describes the `bluetape4k-projects` 1.12.1 release source. Despite the artifact name, it does not provide starter metadata, configuration-properties binding, or general application bootstrapping.

`VirtualThreadAutoConfiguration` is not discovered automatically. The companion executor in `AbstractVirtualThreadController` is shared for the process lifetime and is not closed by that class. Separate scopes in the WebFlux coroutine controller base classes do not automatically carry Reactor or Spring Security context. First check whether the request's existing suspend context is sufficient for new code.

## Source and tests {#sources}

- [Module README](../../../../spring-boot/core/README.md)
- [Module build](../../../../spring-boot/core/build.gradle.kts)
- [`AnnotationExtensions.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/AnnotationExtensions.kt)
- [`PropertyResolverExtensions.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/PropertyResolverExtensions.kt)
- [`RestClientCoroutinesDsl.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/http/RestClientCoroutinesDsl.kt)
- [`AbstractWebClientConfig.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/config/AbstractWebClientConfig.kt)
- [`HttpRequestCapturer.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/filter/HttpRequestCapturer.kt)
- [`DataBufferSupport.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/io/buffer/DataBufferSupport.kt)
- [`ApiExceptionHandler.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/rest/exceptions/ApiExceptionHandler.kt)
- [`SpringObservationSupport.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/observability/SpringObservationSupport.kt)
- [`VirtualThreadAutoConfiguration.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/virtualthread/VirtualThreadAutoConfiguration.kt)
- [`CustomWebClientConfigTest.kt`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/webflux/config/CustomWebClientConfigTest.kt)
- [`SpringObservationSupportTest.kt`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/observability/SpringObservationSupportTest.kt)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Spring Boot Core Capability Map diagram

[![Spring Boot Core Capability Map diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-01.svg)

_Release README: [`spring-boot/core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/core/README.md)_

### Spring WebFlux + Coroutines Request Flow diagram

[![Spring WebFlux + Coroutines Request Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-02.svg)

_Release README: [`spring-boot/core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/core/README.md)_

### RestClient Coroutines DSL Structure diagram

[![RestClient Coroutines DSL Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-03.svg)

_Release README: [`spring-boot/core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/core/README.md)_

### WebClient Dedicated Resource Configuration diagram

[![WebClient Dedicated Resource Configuration diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-04.svg)

_Release README: [`spring-boot/core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/core/README.md)_

<!-- release-readme-diagrams:end -->
