# LettuceSuspendedLoadedMap Write-Behind Drop Fix

**Date**: 2026-05-16
**Issue**: #486
**Branch**: `fix/lettuce-write-behind-drop`

## Root Cause

Two silent failure paths in `LettuceSuspendedLoadedMap`:

### 1. trySend result ignored on retry

When `flushBatch()` encountered a writer failure and the retry count was below
`MAX_DEAD_LETTER_RETRY`, it attempted to requeue each entry via:

```kotlin
writeBehindChannel?.trySend(Triple(k, v, retryCount))
```

The `ChannelResult` returned by `trySend` was silently discarded. If the channel
was full or closed at that moment (e.g., another burst of writes had filled it,
or `close()` had been called), the entry was simply dropped with no log and no
dead-letter record.

### 2. close() cancelled the caller's CoroutineScope

`close()` ended with `scope.cancel()`. When the caller passed a shared scope
(a common pattern for application-wide coroutine supervisors), all coroutines
running in that scope were cancelled — not just the write-behind consumer job.

## Fix

### trySend result check

Extracted a `writeToDeadLetter(batch: Map<K, V>)` helper. The retry branch
now checks the `ChannelResult`:

```kotlin
val dropped = mutableMapOf<K, V>()
entries.forEach { (k, v, _) ->
    val result = writeBehindChannel?.trySend(Triple(k, v, retryCount))
    if (result == null || result.isFailure) {
        log.warn { "Requeue failed for key=$k (attempt $retryCount): channel full or closed" }
        dropped[k] = v
    }
}
if (dropped.isNotEmpty()) {
    writeToDeadLetter(dropped)
}
```

The dead-letter exhaustion path (`retryCount >= MAX_DEAD_LETTER_RETRY`) was
also refactored to call `writeToDeadLetter` instead of duplicating the HSET +
LPUSH logic.

### Scope ownership fix

Added a private `ownedJob` (`SupervisorJob` child of the provided scope) and
`ownedScope` built from it. `writeBehindJob` is launched on `ownedScope`.
`close()` cancels `ownedJob` only:

```kotlin
private val ownedJob = SupervisorJob(parent = scope.coroutineContext[Job])
private val ownedScope = CoroutineScope(scope.coroutineContext + ownedJob)
// ...
override fun close() {
    writeBehindChannel?.close()
    writeBehindJob?.let { job ->
        runBlocking(Dispatchers.IO) {
            withTimeout(...) { job.join() }
        }
    }
    ownedJob.cancel()   // not scope.cancel()
    ...
}
```

## Test Coverage

Two new tests in `LettuceSuspendedLoadedMapTest`:

- `close - 공유 scope를 취소하지 않는다` — passes a shared scope, closes the map,
  asserts `sharedScope.isActive == true`
- `write-behind - writer 실패 후 재시도 소진 시 dead-letter에 기록된다` — always-failing
  writer exhausts `MAX_DEAD_LETTER_RETRY` retries, verifies the key appears in the
  Redis dead-letter list

## Verification

```
:bluetape4k-lettuce:test
322 passing (14.1s) — BUILD SUCCESSFUL
```

## Key Lessons

**Never discard a `ChannelResult` from `trySend`.** A `trySend` on a full or
closed channel returns failure silently. Always check the result and route
undeliverable messages to a dead-letter or log them explicitly.

**The owner of a `CoroutineScope` is the one who should cancel it.** If a class
receives a scope parameter, it should create a child job/scope internally and
cancel only that child on `close()`. Cancelling the provided scope is a contract
violation that affects all callers sharing the scope.
