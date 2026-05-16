# Near-Cache Event Propagation: Root Cause and Per-Backend Verdict

**Issue**: #490  
**Branch**: fix/nearcache-event-propagation  
**Date**: 2026-05-16

## Root Cause

`AbstractSuspendNearJCacheTest` created `suspendNearJCache1`/`2` by calling the
`SuspendNearJCache` **internal constructor** directly:

```kotlin
// BEFORE (broken) — bypasses listener registration
protected val suspendNearJCache1 by lazy {
    SuspendNearJCache(frontCoCache1, backSuspendJCache)
}
```

`SuspendNearJCache` has an `internal constructor(frontCache, backCache)` and a
companion `operator fun invoke(frontCache, backCache)` with identical parameter types.
Because testFixtures live in the **same module** as main source, the `internal`
constructor is visible, and Kotlin resolves `SuspendNearJCache(front, back)` to the
**constructor**, not the companion `invoke`. The companion `invoke` is the only path
that registers `SuspendJCacheEntryEventListener` on the back cache, so no events
ever propagated → every cross-cache propagation assertion timed out.

The sync `AbstractNearJCacheTest` was unaffected because it calls
`NearJCache(nearCacheCfg, backCache)` which maps to the companion `invoke(nearCacheCfg,
backCache)` — different parameter types from the constructor — so there was never ambiguity.

## Fix

Added an `open fun createSuspendNearJCache(front, back)` hook to the test fixture,
with default body `SuspendNearJCache.invoke(front, back)` (explicit companion call):

```kotlin
// AFTER (correct) — explicit companion invoke registers listener
protected open fun createSuspendNearJCache(
    front: SuspendJCache<String, Any>,
    back: SuspendJCache<String, Any>,
): SuspendNearJCache<String, Any> = SuspendNearJCache.invoke(front, back)

protected val suspendNearJCache1 by lazy { createSuspendNearJCache(frontCoCache1, backSuspendJCache) }
protected val suspendNearJCache2 by lazy { createSuspendNearJCache(frontCoCache2, backSuspendJCache) }
```

The `open` hook allows Hazelcast (and any future backend) to override with
`SuspendNearJCache.withoutListener(front, back)` when listener registration is
structurally impossible.

## Per-Backend Verdicts

### Lettuce (cache-lettuce)
**Verdict A — re-enabled.**  
`LettuceJCache.dispatchEvent()` is in-process (`ConcurrentHashMap` of listeners, called
synchronously after each mutation). Once the fixture bug was fixed, all 19 test
methods pass. Removed `@Disabled`.

### Redisson (cache-redisson)
**Verdict A — re-enabled.**  
After the fixture fix, all tests pass. The `SuspendNearJCache` implementation already
works around Redisson's `removeAll`/`replace` event gaps via explicit per-key removes.
Prior `@Disabled("버그가 많아 일단 테스트에서 제외한다.")` was stale. Removed `@Disabled`.

### Hazelcast sync (cache-hazelcast — HazelcastNearJCacheTest)
**Verdict B — kept disabled with precise reason.**  
Hazelcast cluster-distributes `MutableCacheEntryListenerConfiguration` by Java
serialization. `JCacheEntryEventListener` holds a reference to the front `JCache`
(Caffeine) which is not `Serializable`. Registration throws
`HazelcastSerializationException: NotSerializableException`. Updated `@Disabled`
message with `Tracked: #490`.

### Hazelcast suspend (cache-hazelcast — HazelcastSuspendNearJCacheTest)
**Verdict B — kept disabled with precise reason.**  
Same structural constraint as sync: `SuspendJCacheEntryEventListener` captures a
`CaffeineSuspendJCache` (not `Serializable`). Registration throws
`HazelcastSerializationException: NotSerializableException(CaffeineSuspendJCache)`.
Updated `@Disabled` message with `Tracked: #490`.

## Lesson: Kotlin Companion Invoke vs Internal Constructor Ambiguity

When a class has both an `internal constructor(A, B)` and a companion
`operator fun invoke(A, B)` with **identical parameter types**, code in the same
module calling `ClassName(a, b)` resolves to the **constructor**, not the companion
`invoke`. This is the opposite of what you might assume if you only looked at
visibility from outside the module.

**Always use `ClassName.invoke(...)` or `ClassName.Companion.invoke(...)` explicitly**
when the companion invoke is the intended entry point and the constructor must be
bypassed. Alternatively, make the constructor `private` to eliminate the ambiguity.

## Files Changed

| File | Change |
|---|---|
| `cache/cache-core/src/testFixtures/.../AbstractSuspendNearJCacheTest.kt` | Added `createSuspendNearJCache` hook; default calls `SuspendNearJCache.invoke(...)` |
| `cache/cache-lettuce/src/test/.../LettuceSuspendNearJCacheTest.kt` | Removed `@Disabled` |
| `cache/cache-redisson/src/test/.../RedissonSuspendNearJCacheTest.kt` | Removed `@Disabled` |
| `cache/cache-hazelcast/src/test/.../HazelcastNearJCacheTest.kt` | Updated `@Disabled` with precise reason + `Tracked: #490` |
| `cache/cache-hazelcast/src/test/.../HazelcastSuspendNearJCacheTest.kt` | Updated `@Disabled` with precise reason + `Tracked: #490` |

## Test Results

- `bluetape4k-cache-core:test --tests "*NearJCache*"` → BUILD SUCCESSFUL
- `bluetape4k-cache-lettuce:test --tests "*NearJCache*"` → BUILD SUCCESSFUL (was failing)
- `bluetape4k-cache-redisson:test --tests "*NearJCache*"` → BUILD SUCCESSFUL (was disabled)
- `bluetape4k-cache-hazelcast:test --tests "*NearJCache*"` → BUILD SUCCESSFUL (Hazelcast tests skipped per @Disabled)
