---
title: Lazy messages and failure isolation
description: Understand exact evaluation semantics for level guards, suppliers, markers, and error overloads.
manualId: bluetape4k-logging
chapterId: lazy-messages
---

# Lazy messages and failure isolation

String interpolation, JSON conversion, and collection summaries should not run for a disabled level. Lambda extensions evaluate messages behind the level guard.

## Evaluation order

```kotlin
log.debug { "Loaded ${records.size} rows: ${records.take(3)}" }
```

1. Check `isDebugEnabled`.
2. Invoke the supplier only when enabled.
3. Convert a `null` result to the string `"null"`.
4. If the supplier throws, emit fallback text instead of interrupting business flow.

`logMessageSafe` defaults to `Fail to generate log message.` and appends the exception text. This isolates service behavior from diagnostic-code failure, but can hide a supplier bug; make fallback occurrences observable in tests and operations.

## Errors and markers

Every level has message-only, cause-plus-message, and marker-plus-cause-plus-message overloads.

```kotlin
log.warn(validationError) { "Rejected requestId=$requestId" }
log.error(securityMarker, failure) { "Authorization failed" }
```

WARN and ERROR helpers prefix messages with `🔥`. SLF4J level remains the severity contract; do not build alerts by parsing the prefix alone.

## Avoid these patterns

```kotlin
// The eager computation already happened.
log.debug(expensiveSnapshot())

// Lazy does not make secrets safe.
log.info { "token=$token" }
```

Laziness changes evaluation time, not redaction. Capturing a mutable object records its value at evaluation time, so extract the required immutable fields first.

## Source and tests

- [`Slf4jExtensions.kt`](../../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/Slf4jExtensions.kt)
- [`Slf4jExtensionsTest.kt`](../../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/Slf4jExtensionsTest.kt)
- [`Slf4jMdcExtensions.kt`](../../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/Slf4jMdcExtensions.kt)
- [`Slf4jMdcExtensionsTest.kt`](../../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/Slf4jMdcExtensionsTest.kt)

Continue with correlation boundaries in [Scoped MDC](./scoped-mdc.md).
