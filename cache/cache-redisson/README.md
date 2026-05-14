# Module bluetape4k-cache-redisson

English | [한국어](./README.ko.md)

`bluetape4k-cache-redisson` provides Redisson-backed cache adapters for the bluetape4k cache APIs. It focuses on JCache integration, coroutine-friendly wrappers, Redisson `RLocalCachedMap` near caches, and Redis-backed memoizers.

## Package / Import Stability

The cache folder reorganization moved this module under `cache/cache-redisson/`,
but the Gradle project name, Maven artifact ID, and Kotlin packages remain stable:

- Gradle project: `:bluetape4k-cache-redisson`
- Maven artifact: `io.github.bluetape4k:bluetape4k-cache-redisson`
- Kotlin package root: `io.bluetape4k.cache`

No user import migration is required for the reorganization.

## Provided APIs

| API | Package | Purpose |
| --- | --- | --- |
| `RedissonJCaching` | `jcache` | Redisson JCache provider helper |
| `RedissonSuspendJCache<K, V>` | `jcache` | `SuspendJCache` wrapper over Redisson JCache |
| `RedissonNearCache<V>` | `nearcache` | synchronous `NearCacheOperations` backed by `RLocalCachedMap` |
| `RedissonSuspendNearCache<V>` | `nearcache` | suspend `SuspendNearCacheOperations` backed by `RLocalCachedMap` |
| `RedissonCaches` | root package | factory methods for JCache, suspend JCache, and near caches |
| `RedissonMemoizer<T, R>` | `memoizer` | synchronous Redis-backed memoizer |
| `RedissonAsyncMemoizer<T, R>` | `memoizer` | `CompletableFuture`/`CompletionStage` memoizer |
| `RedissonSuspendMemoizer<T, R>` | `memoizer` | coroutine memoizer with per-key in-flight sharing |

This module does not expose RESP3 hybrid near-cache classes. Use the actual `RedissonNearCache` / `RedissonSuspendNearCache` APIs when you want Redisson-managed local caching.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-redisson:$bluetape4kVersion")
}
```

## Recommended Scenarios

- You already use Redisson and want cache APIs aligned with bluetape4k `JCache`, `SuspendJCache`, `NearCacheOperations`, or `SuspendNearCacheOperations`.
- You need a Redis-backed memoizer that shares in-flight work inside one JVM and stores completed values in `RMap`.
- You want Redisson's `RLocalCachedMap` invalidation behavior instead of maintaining a separate local cache plus Redis synchronization layer.
- You need coroutine call sites to await Redisson async operations without blocking application threads.

## Anti-Patterns

- Do not treat `close()` as data deletion. `close()` releases the wrapper/provider resource; use `clear()` or `clearAll()` when entries must be removed.
- Do not document or call non-existent RESP3 helper APIs from this module. The public surface is the Redisson/JCache/RLocalCachedMap set listed above.
- Do not use a memoizer for non-idempotent side effects. Failed or cancelled evaluator calls are not cached as successful values, so retries can re-run the evaluator.
- Do not expect `RedissonSuspendMemoizer` in-flight sharing to cross JVM boundaries. Redis stores completed values, but the in-flight coordination map is process-local.

## Examples

### Suspend JCache

```kotlin
import io.bluetape4k.cache.RedissonCaches
import javax.cache.configuration.MutableConfiguration

val cache = RedissonCaches.suspendJCache<String, String>(
    redisson = redissonClient,
    cacheName = "users",
    configuration = MutableConfiguration<String, String>().apply {
        setTypes(String::class.java, String::class.java)
    },
)

cache.put("u:1", "debop")
val user = cache.get("u:1")
// user == "debop"

cache.close()
```

### Native Redisson Near Cache

```kotlin
import io.bluetape4k.cache.RedissonCaches
import io.bluetape4k.cache.nearcache.RedissonNearCacheConfig

val nearCache = RedissonCaches.nearCache<String>(
    redisson = redissonClient,
    config = RedissonNearCacheConfig(cacheName = "products"),
)

nearCache.put("p:1", "keyboard")
val product = nearCache.get("p:1")
nearCache.clearLocal()   // local tier only
nearCache.clearAll()     // local + Redis tiers
nearCache.close()
```

### Suspend Memoizer

```kotlin
import io.bluetape4k.cache.memoizer.suspendMemoizer
import org.redisson.client.codec.IntegerCodec

val map = redissonClient.getMap<Int, Int>("memoizer:squares", IntegerCodec())
val memoizer = map.suspendMemoizer { key ->
    expensiveSquare(key)
}

val first = memoizer(7)   // computes and stores 49
val second = memoizer(7)  // returns cached 49
```

If the evaluator fails or is cancelled, the in-flight entry is completed exceptionally and removed. The next call for the same key starts a fresh computation instead of being stuck on a stale failed deferred.

## Lifecycle Notes

- `RedissonSuspendJCache.close()` delegates to the wrapped Redisson JCache and does not delete stored entries.
- `RedissonSuspendNearCache.close()` destroys the local cached map wrapper and marks the wrapper closed; entry deletion remains the responsibility of `clearAll()`.
- Suspend close paths preserve `CancellationException` rather than swallowing it through `runCatching`.

## Verification Focus

Relevant test coverage includes:

- JCache CRUD and close/data-preservation behavior.
- Native near-cache read/write/clear/stat behavior.
- Memoizer thread, virtual-thread, and coroutine contention using `MultithreadingTester`, `StructuredTaskScopeTester`, and `SuspendedJobTester`.
- Suspend memoizer evaluator failure, explicit cancellation, and real `Job.cancel()` recovery.
