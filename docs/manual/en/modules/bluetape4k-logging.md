---
manualId: bluetape4k-logging
title: "Kotlin Logging and MDC"
description: Boundaries and lifecycle for lazy SLF4J logging, scoped MDC, coroutine propagation, and optional asynchronous logging.
kind: library
group: foundation
learningOrder: 130
---

# Kotlin Logging and MDC

## Problem {#problem}

`bluetape4k-logging` treats logging as more than a call syntax: when is a message evaluated, how far does request context travel, and who closes asynchronous delivery? The application still owns the SLF4J provider and appenders.

![Responsibility map for logger creation, lazy messages, MDC, and asynchronous delivery](../../assets/logging/logger-api-map.svg)

## When to use {#when-to-use}

Use it for consistent logger naming, lazy messages, and correlation context scoped to an operation. Do not add wrappers when plain SLF4J already expresses the complete contract.

## API by task {#api-by-task}

| Requirement | Default choice | Boundary to verify |
| --- | --- | --- |
| Class or companion logger | `KLogging` | The logger initializes on first access. |
| Top-level or explicit name | `KotlinLogging.logger` | Blank names are rejected. |
| Expensive message | lambda `debug {}` and peers | Disabled levels do not evaluate the supplier. |
| Correlation context in synchronous code | `withLoggingContext` | Choose nested-value restoration policy. |
| Correlation context in suspend code | `withCoroutineLoggingContext` | `MDCContext` bridges dispatcher switches. |
| Decouple caller from emission | `KLoggingChannel` | Own buffering, collection, close, and possible loss. |

Start ordinary services with `KLogging` and scoped MDC. Choose `KLoggingChannel` only after synchronous appenders are measured as a bottleneck and shutdown handling for pending events is explicit.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-logging")
    runtimeOnly("ch.qos.logback:logback-classic:<version>")
}
```

Select exactly one SLF4J provider in the application. This module does not configure levels, encoders, destinations, or retention.

## Concepts {#concepts}

Logger creation, lazy event construction, MDC context, and optional background delivery are separate responsibilities. This prevents provider policy and channel lifecycle from hiding inside a convenience helper.

## Quick start {#quick-start}

```kotlin
class OrderService {
    companion object : KLogging()

    suspend fun load(orderId: String): Order =
        withCoroutineLoggingContext("orderId" to orderId) {
            log.debug { "Loading order" }
            repository.load(orderId)
        }
}
```

The message lambda runs only when DEBUG is enabled. `orderId` travels with the coroutine context and is restored when the scope ends.

## Patterns {#patterns}

Sanitize correlation keys at the transport boundary and keep them around the smallest owning operation. A message supplier should compute a value without introducing side effects.

## Integrations {#integrations}

The module integrates SLF4J providers, `MDCContext` from `kotlinx-coroutines-slf4j`, and Ktor or Spring request lifecycles. It does not create traces or metrics.

## Configuration {#configuration}

The host application owns levels, encoders, appenders, destinations, retention, and MDC output patterns. Keep exactly one provider on the runtime classpath.

## Failures {#failures}

Supplier failure becomes fallback text. MDC restoration runs in `finally`. Channel close cancels rather than drains, and events after close are dropped.

## Operations {#operations}

Observe fallback messages, duplicate providers or appenders, missing MDC fields, appender latency, and channel shutdown omissions. Remove secrets before they enter suppliers or MDC.

## Testing {#testing}

```bash
./gradlew :bluetape4k-logging:test --no-configuration-cache
```

Representative tests cover naming, level guards, supplier failure, nested MDC, coroutine propagation, and channel close.

## Workshops {#workshops}

No dedicated logging workshop is registered. The smallest exercise applies correlation context at a Ktor or Spring request boundary and tests nested restoration plus shutdown.

## Limitations {#limitations}

Logging is not tracing, audit storage, or durable event delivery. MDC cannot follow thread switches without a bridge, and the async channel does not guarantee pending events at shutdown.

## Learning path

1. [Logger foundation](./bluetape4k-logging/logger-foundation.md) — names and provider boundary
2. [Lazy messages](./bluetape4k-logging/lazy-messages.md) — level guards and supplier failure
3. [Scoped MDC](./bluetape4k-logging/scoped-mdc.md) — nested restoration and exceptional cleanup
4. [Coroutine MDC](./bluetape4k-logging/coroutine-mdc.md) — suspension and child propagation
5. [Async channel](./bluetape4k-logging/async-channel.md) — buffer, collector, close, and event loss
6. [Operations & recipes](./bluetape4k-logging/operations-recipes.md) — configuration, redaction, diagnosis, and tests

## Contract boundary

The manual promotes only behavior verified in current source and representative tests: naming, lazy evaluation, fallback messages, MDC restore/remove, the coroutine bridge, and channel close behavior. README timing examples are not controlled benchmarks and are not library guarantees.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Logging class structure for KLogging, KotlinLogging, MDC helpers, and async channel logging

[![Logging class structure for KLogging, KotlinLogging, MDC helpers, and async channel logging](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-diagram-01.svg)

_Release README: [`bluetape4k/logging/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/logging/README.md)_

### Logging processing flow for level guards, lazy message suppliers, MDC context, and SLF4J emission

[![Logging processing flow for level guards, lazy message suppliers, MDC context, and SLF4J emission](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-diagram-02.svg)

_Release README: [`bluetape4k/logging/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/logging/README.md)_

### KLoggingChannel async logging sequence with SharedFlow buffering and background collector emission

[![KLoggingChannel async logging sequence with SharedFlow buffering and background collector emission](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-logging-sequence-01.svg)

_Release README: [`bluetape4k/logging/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/logging/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module source](../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging)
- [Representative tests](../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging)
- [README](../../../../bluetape4k/logging/README.md)
