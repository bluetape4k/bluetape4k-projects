---
manualId: bluetape4k-logging
title: Kotlin logging and MDC support
description: Lazy SLF4J logging, scoped MDC, coroutine context propagation, and optional channel-backed asynchronous logging.
kind: library
group: foundation
---

# Kotlin logging and MDC support

## Problem {#problem}

Backend code needs a consistent logger declaration, lazy message evaluation, and request context that survives coroutine boundaries without leaking into the next request. High-volume paths may also need bounded asynchronous log delivery. `bluetape4k-logging` builds these contracts on SLF4J and kotlin-logging.

## When to use {#when-to-use}

Use `KLogging` for class/companion loggers, `KotlinLogging.logger {}` for package-level functions, and scoped MDC helpers when logs must carry trace, request, tenant, or user identifiers. Use coroutine MDC helpers inside suspend code. Choose `KLoggingChannel` only after measuring that synchronous logging is a bottleneck and deciding how queued messages should behave during shutdown.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-logging")
    runtimeOnly("ch.qos.logback:logback-classic:<version>")
}
```

The application still chooses one SLF4J provider. Do not ship multiple competing providers.

## Concepts {#concepts}

`KLogging` exposes a lazily initialized SLF4J `Logger` named from the owning type. Lambda-based extensions avoid formatting messages when a level is disabled. `withLoggingContext` installs MDC key/value pairs for one block and restores or removes prior values in `finally`. `withCoroutineLoggingContext` carries the context through coroutine suspension.

`KLoggingChannel` introduces a queue and runtime lifecycle; unlike ordinary logging, delivery can lag behind the caller.

## Quick start {#quick-start}

```kotlin
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.withLoggingContext

class OrderService {
    companion object : KLogging()

    fun load(orderId: String) = withLoggingContext("orderId" to orderId) {
        log.info { "Loading order" }
    }
}
```

The MDC helper restores the previous value after the block, including exceptional exits.

## API by task {#api-by-task}

| Task | API |
| --- | --- |
| Logger in a class or companion | `KLogging` |
| Logger in top-level code | `KotlinLogging.logger {}` |
| Lazy SLF4J message | `log.debug { ... }`, `log.info { ... }` extensions |
| One or many scoped MDC entries | `withLoggingContext` overloads |
| MDC across suspend/async boundaries | `withCoroutineLoggingContext` |
| Queue-backed asynchronous logging | `KLoggingChannel` |

## Patterns {#patterns}

Add correlation data at the transport boundary and keep the MDC scope around the operation that owns it. Use stable low-cardinality keys such as `traceId`, `requestId`, and `tenantId`; do not place secrets or unbounded payloads in MDC. Keep log messages lazy and structured enough for the selected backend encoder.

## Integrations {#integrations}

The module targets SLF4J and works with Logback or another chosen provider. Coroutine MDC support integrates SLF4J MDC with Kotlin coroutine context. Observability modules can populate trace identifiers, but logging does not create spans or metrics by itself.

## Configuration {#configuration}

Log level, encoder, appenders, retention, and destination belong to the SLF4J provider configuration. `withLoggingContext` has `restorePrevious=true` by default. Channel logging adds queue/runtime options documented by `KLoggingChannel`; select them from measured throughput and loss tolerance rather than copying a global default.

## Failures {#failures}

Logger backend failures follow the selected provider. Scoped MDC cleanup runs in `finally`; map cleanup callbacks deliberately ignore cleanup exceptions so they do not replace the business exception. A bounded async queue can reject, block, or lose work according to its policy, and shutdown before draining can leave messages unwritten.

## Operations {#operations}

Monitor log volume, dropped/queued message count for channel logging, appender latency, and disk/network backpressure. Redact secrets before values enter MDC or message lambdas. Close asynchronous logging resources during application shutdown and test the drain behavior explicitly.

## Testing {#testing}

`KLoggingTest` and `KotlinLoggingTest` cover logger naming and creation. `MdcSupportTest` and `MdcSupportCoroutinesTest` verify restoration and propagation. `KLoggingChannelTest` covers the channel lifecycle.

```bash
./gradlew :bluetape4k-logging:test --no-configuration-cache
```

## Workshops {#workshops}

No dedicated logging workshop is registered. Ktor and Spring observability examples are the best place to connect request IDs and tracing context to this module. A focused exercise should verify nested MDC restoration and coroutine `async` propagation.

## Limitations {#limitations}

MDC is thread-local state underneath the coroutine bridge; plain thread switches without the bridge can lose context. Logging is not tracing, auditing, or secure secret storage. Asynchronous delivery improves caller latency only by moving work to a queue and therefore adds buffering and shutdown trade-offs.

## Sources {#sources}

- [Module README and examples](../../../../bluetape4k/logging/README.md)
- [`KLogging` source](../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/KLogging.kt)
- [Scoped MDC implementation](../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/MdcSupport.kt)
- [Coroutine/channel logging source](../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/coroutines)
- [Logging tests](../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging)
