---
title: Scoped MDC and nested restoration
description: Install thread-local MDC values for one operation, then restore or remove them precisely.
manualId: bluetape4k-logging
chapterId: scoped-mdc
---

# Scoped MDC and nested restoration

MDC adds correlation fields to log lines, but its underlying state is thread-local and mutable. Separating put and cleanup responsibility easily leaks one request into the next.

![Lifecycle of outer, inner, and restored MDC state](../../../assets/logging/mdc-scope-lifecycle.svg)

## What the scope owns

```kotlin
withLoggingContext(
    "traceId" to "trace-100",
    "tenantId" to tenantId,
) {
    log.info { "Load account" }
}
```

`restorePrevious=true` is the default. The helper remembers each prior value and restores it in `finally` after success or failure. Keys without a previous value are removed.

## Nesting and policy

```kotlin
MDC.put("traceId", "outer")
withLoggingContext("traceId" to "inner") {
    check(MDC.get("traceId") == "inner")
}
check(MDC.get("traceId") == "outer")
```

With `restorePrevious=false`, the scope removes installed keys on exit. Keep the default for a nested operation that must return to outer values. A `null` map or vararg value is not installed.

## Exceptions and cleanup

Cleanup runs after a business exception. A map cleanup callback exception is swallowed so it cannot replace the primary failure. This preserves the main exception; it does not make cleanup defects harmless.

## Values appropriate for MDC

- small, stable request, trace, span, or tenant correlation keys
- identifiers already sanitized
- fields actually emitted by the log pattern or structured encoder

Do not store passwords, tokens, raw payloads, or unbounded collections. Setting a field is insufficient: Logback needs `%X{traceId}` or a corresponding structured mapping to emit it.

## Source and tests

- [`MdcSupport.kt`](../../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/MdcSupport.kt)
- [`MdcSupportTest.kt`](../../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/MdcSupportTest.kt)

Use [Coroutine MDC](./coroutine-mdc.md) when suspend code can switch threads.
