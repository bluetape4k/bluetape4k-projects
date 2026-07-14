---
title: Retry, failures, and lifecycle
description: Verify Near Cache retry, read fallback, write exceptions, coroutine cancellation, and close behavior.
manualId: bluetape4k-cache-core
chapterId: resilience-failures-lifecycle
---

# Retry, failures, and lifecycle

## Retry repeats remote cache operations

`withResilience` decorates a common Near Cache interface with Resilience4j retry.

```kotlin
val resilient = cache.withResilience {
    retryMaxAttempts = 3
    retryWaitDuration = Duration.ofMillis(200)
    retryExponentialBackoff = true
    getFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL
}
```

The defaults are three attempts, a 500 ms initial wait, and exponential backoff. Attempts and duration must be positive. Verify operation idempotency before retrying side effects.

## Read and write failure policies

| Operation | `RETURN_FRONT_OR_NULL` | `PROPAGATE_EXCEPTION` |
| --- | --- | --- |
| `get` | log and return `null` | propagate the last exception |
| `getAll` | log and return an empty map | propagate the last exception |
| `containsKey` | log and return `false` | propagate the last exception |

Fallback makes a missing entry indistinguishable from a failed backend. Use it only when availability has priority, and monitor an error counter or the warning logs.

Mutation operations such as `put`, `replace`, `remove`, and `clearAll` propagate after retry exhaustion. Silently dropping a write could leave local and back tiers inconsistent.

## Coroutine cancellation

`ResilientSuspendNearCacheDecorator` excludes `CancellationException` from retry. It rethrows cancellation even under `RETURN_FRONT_OR_NULL`, including cancellation during `put` and `close`.

```kotlin
val job = launch { resilient.get("slow-key") }
job.cancelAndJoin()
```

Converting cancellation to a fallback would keep cache work alive after its parent scope has stopped.

## Close and ownership

The blocking decorator logs and ignores ordinary delegate close failures. The suspend decorator does the same for ordinary exceptions but rethrows cancellation. Ignoring a close error does not prove that connections and threads were released; inspect provider metrics and shutdown logs.

Confirm whether a factory owns the remote client or only the cache wrapper. Avoid both closing a shared client too early and leaving an unowned client open.

## Retry does not stop a cache stampede

Retry repeats a failed remote call. It does not merge concurrent misses for a hot key and may amplify a slow backend. Combine same-key memoization or provider loader merging with bounded retry, timeout, and source-store protection.

## Sources and tests

- [`NearCacheResilienceConfig.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheResilienceConfig.kt)
- [`ResilientNearCacheDecorator.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheDecorator.kt)
- [`ResilientSuspendNearCacheDecorator.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecorator.kt)
- [`ResilientNearCacheDecoratorTest.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheDecoratorTest.kt)
- [`ResilientSuspendNearCacheDecoratorTest.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecoratorTest.kt)
