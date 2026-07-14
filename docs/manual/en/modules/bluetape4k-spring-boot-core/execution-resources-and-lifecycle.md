---
title: Execution resources and lifecycle
description: Explains creation, configuration, ownership, and shutdown of dedicated WebClient loops, controller scopes, and virtual-thread executors.
manualId: bluetape4k-spring-boot-core
chapterId: execution-resources-and-lifecycle
---

# Execution resources and lifecycle

## Isolate WebClient resources from the server

`AbstractWebClientConfig` creates dedicated `LoopResources`, `ReactorResourceFactory`, `ReactorClientHttpConnector`, `ExchangeStrategies`, and `WebClient` beans. `isUseGlobalResources = false` separates the client from global Reactor resources used by a server or another client.

```kotlin
@Configuration
class PartnerWebClientConfig : AbstractWebClientConfig() {
    override val threadCount: Int = 8
    override val connectTimeoutMillis: Int = 2_000
    override val shutdownTimeout: Duration = Duration.ofSeconds(10)
    override val maxInMemorySize: Int = 4 * 1024 * 1024
}
```

Isolation reduces shared failure impact but consumes additional threads and direct memory. Use separate loops at boundaries with a clear traffic or isolation requirement.

## Timeouts and SSL

The response timeout defaults to `PT30S` and can be changed with `bluetape4k.webclient.response-timeout`. Override the subclass property for connect timeout.

The default `sslContext()` validates server certificates using the JDK trust store. `insecureSslContext()` trusts every certificate and belongs only in development or tests. Connect private CAs through a custom trust store and `SslContext` in production.

## Codec memory limit

`exchangeStrategies()` changes only the default codec `maxInMemorySize`, which defaults to 16 MiB. It is a limit for what codecs aggregate in memory, not a total size limit for every streaming payload. Consider streaming or payload design before increasing it for large JSON or multipart data.

## Controller coroutine scopes

`AbstractCoroutineDefaultController`, `AbstractCoroutineIOController`, and `AbstractCoroutineVTController` create separate `CoroutineScope` instances with `SupervisorJob`. Their `@PreDestroy` method cancels the job when the Spring bean is destroyed.

These scopes are not children of the request coroutine. Request cancellation and Reactor or Spring Security context do not follow automatically. Run request-lifetime work in the current suspend context, and hand longer work to an explicitly owned application service scope.

## Virtual-thread configuration

`VirtualThreadAutoConfiguration` registers a virtual-thread-enabled `SimpleAsyncTaskExecutor` only when no `AsyncTaskExecutor` bean exists.

```kotlin
@Import(VirtualThreadAutoConfiguration::class)
class AsyncConfiguration
```

There is no auto-configuration imports metadata, so `@Import` is required. `@ConditionalOnMissingBean` makes the configuration back off when the application already owns an executor.

`AbstractVirtualThreadController.virtualThreadExecutor` is a process-wide companion object created by `newVirtualThreadPerTaskExecutor()`. The class has no close hook. Prefer an application-owned executor bean when shutdown control matters.

## Source and tests

- [`AbstractWebClientConfig.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/config/AbstractWebClientConfig.kt)
- [`AbstractCoroutineDefaultController.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/controller/AbstractCoroutineDefaultController.kt)
- [`AbstractCoroutineIOController.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/controller/AbstractCoroutineIOController.kt)
- [`AbstractCoroutineVTController.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/controller/AbstractCoroutineVTController.kt)
- [`VirtualThreadAutoConfiguration.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/virtualthread/VirtualThreadAutoConfiguration.kt)
- [`AbstractVirtualThreadController.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/virtualthread/AbstractVirtualThreadController.kt)
- [`CustomWebClientConfigTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/webflux/config/CustomWebClientConfigTest.kt)

## Next chapter

[Testing and ecosystem paths](./testing-and-ecosystem-paths.md) maps focused test anchors and the next modules to study.
