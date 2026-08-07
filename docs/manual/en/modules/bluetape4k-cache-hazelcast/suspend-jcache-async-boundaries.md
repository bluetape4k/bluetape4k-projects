---
title: Suspend JCache and async boundaries
description: Understand Hazelcast ICache async operations, Dispatchers.IO fallback, bulk processing, and compound-operation atomicity.
manualId: bluetape4k-cache-hazelcast
chapterId: suspend-jcache-async-boundaries
---

# Suspend JCache and async boundaries

## Use ICache async operations when unwrap succeeds

`HazelcastSuspendJCache` tries to unwrap its JCache as `ICache`. When successful, `get`, `put`, `remove`, and `replace` use native async operations followed by coroutine `await`. Otherwise, the equivalent standard JCache call runs on `Dispatchers.IO`.

```kotlin
val cache = HazelcastCaches.suspendJCache<String, User>(hazelcast, "users-v1")

cache.put("42", user)        // ICache.putAsync(...).await()
val loaded = cache.get("42") // ICache.getAsync(...).await()
```

A `suspend` signature does not mean every operation is a native Hazelcast async command. `containsKey`, `clear`, `putAll`, keyed `getAll`, and listener registration use blocking or direct JCache paths.

## Bulk paths differ

`putAll` runs one standard JCache bulk call on the IO dispatcher. `putAllFlow` instead collects one `putAsync` deferred per entry and waits with `joinAll` when `ICache` is available. A very large flow also creates a large deferred list before completion, so batch input and test cancellation timing.

## getAndPut is not one Hazelcast operation

In 1.12.1, `getAndPut` is implemented as `get(key).also { put(key, value) }`. Another member can change the entry between those two calls, making the returned old value differ from the actual replacement target.

Do not rely on this wrapper when the operation must be atomic. Evaluate a Hazelcast server-side atomic operation or entry processor instead. `getAndRemoveAsync` and `getAndReplaceAsync` use native compound operations when `ICache` is available.

## Close and cancellation

`close` closes the JCache proxy on the IO dispatcher, not the `HazelcastInstance`. The release suite exercises the shared suspend JCache contract but does not pin every disconnect and cancellation timing. Add application-level tests for cancellation during large bulk operations and client reconnects.

## Sources and tests

- [`HazelcastSuspendJCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/jcache/HazelcastSuspendJCache.kt)
- [`HazelcastSuspendJCacheTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/jcache/HazelcastSuspendJCacheTest.kt)
- [`HazelcastCachesTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/HazelcastCachesTest.kt)
