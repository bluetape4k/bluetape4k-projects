# LettuceLoadedMap getAll MGET Fallback Fix

**Date**: 2026-05-16
**Issue**: #485
**Branch**: `fix/lettuce-getall-fallback`

## Root Cause

`LettuceLoadedMap.getAll()` and `LettuceSuspendedLoadedMap.getAll()` both used:

```kotlin
val values = runCatching { commands.mget(*redisKeys) }
    .onFailure { log.warn(it) { "Redis MGET 실패, loader fallback: ..." } }
    .getOrNull() ?: emptyList()
```

When MGET failed, `values` became `emptyList()`. The subsequent `forEachIndexed`
loop never iterated, so `missedKeys` remained empty. The loader was never invoked
and callers received an empty result instead of fetched values.

The log message said "loader fallback" but the code path made it impossible.

## Fix

### Sync (LettuceLoadedMap.kt)

```kotlin
val mgetResult = runCatching { commands.mget(*redisKeys) }
    .onFailure { log.warn(it) { "Redis MGET 실패, loader fallback: ..." } }
    .getOrNull()

val missedKeys: MutableList<K>
if (mgetResult == null) {
    // MGET failed entirely — treat all requested keys as cache misses
    missedKeys = keyList.toMutableList()
} else {
    missedKeys = mutableListOf()
    mgetResult.forEachIndexed { i, kv ->
        if (kv != null && kv.hasValue()) result[keyList[i]] = kv.value
        else missedKeys.add(keyList[i])
    }
}
```

### Suspend (LettuceSuspendedLoadedMap.kt)

Same logic, plus replaced `runCatching { asyncCommands.mget(...).await() }` (which
swallows `CancellationException`) with explicit try/catch that rethrows
`CancellationException`:

```kotlin
val mgetResult = try {
    asyncCommands.mget(*redisKeys).await()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    log.warn(e) { "Redis MGET 실패, loader fallback: ..." }
    null
}
```

Also replaced the SETEX `runCatching` block with the same explicit try/catch.

## Test Coverage

New tests in `LettuceLoadedMapTest` and `LettuceSuspendedLoadedMapTest`:

- `getAll - 모든 키가 캐시 미스인 경우 loader로 모두 처리한다` — all keys absent from
  Redis, verifies all keys go through the loader and results are returned
- `getAll - 일부 캐시 미스 키는 loader로 Read-through한다` (suspend) — partial miss
  scenario added to the suspend test class (was missing entirely)

Note: the `mgetResult == null` path (MGET throws) is verified by static analysis.
Integration-level simulation of MGET failure requires a mock-based unit test or
a broken-connection fixture, which can be added as a follow-up.

## Verification

```
:bluetape4k-lettuce:test
67 passing (6.2s) — BUILD SUCCESSFUL
```

## Key Lesson

**`getOrNull() ?: emptyList()` silently converts an error into an empty result.**
When the intent is "treat failure as all-miss", populate `missedKeys` explicitly
from `keyList`. Never infer missed keys by iterating a fallback empty collection.

**`runCatching {}` must not wrap `suspend` calls.** It swallows
`CancellationException`, breaking coroutine structured concurrency. Use explicit
try/catch with `CancellationException` rethrow in all suspend paths.
