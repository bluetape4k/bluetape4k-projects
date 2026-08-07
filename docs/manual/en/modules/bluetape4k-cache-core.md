---
manualId: bluetape4k-cache-core
title: "Cache Abstractions and Memoization"
description: "Choose the shared contracts for local caches, JCache, memoizers, and two-tier Near Caches, then verify concurrency, failures, and lifecycle behavior."
kind: library
group: caching
learningOrder: 500
---

# Cache Abstractions and Memoization

## Capabilities {#problem}

`bluetape4k-cache-core` provides the contracts used before choosing a distributed provider: helpers for Caffeine, Cache2k, and Ehcache; JCache creation and configuration; the coroutine-oriented `SuspendJCache`; sync, async, and suspend memoizers; and common Near Cache interfaces that combine a local front cache with a remote back cache.

These APIs make ownership explicit: who fills a miss, whether same-key computations are merged, which tier is cleared, and whether a backend failure is propagated. A cache entry is still not source data. A cache `put` may replicate between cache tiers, but it is not persistence write-through to a database.

## Decisions before adoption {#when-to-use}

- For repeated work inside one JVM, choose the smallest local provider or memoizer API.
- Use `SuspendJCache` or a suspend memoizer on coroutine paths; a suspend signature alone does not prove non-blocking I/O.
- Distinguish caller-owned cache-aside from loader-owned JCache read-through.
- Configure capacity and expiry explicitly. The default JCache configuration is eternal.
- Move to `cache-lettuce` or `cache-redisson` when processes must share values or invalidate local entries.
- For hot keys, verify same-key miss merging through a memoizer or the chosen provider loader.

## Coordinates {#coordinates}

Consumers manage only the central BOM version, not individual Caffeine, Cache2k, Ehcache, or JCache versions.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-core")
}
```

Cache2k and Ehcache implementations are `compileOnly` dependencies in 1.12.1. Add the selected provider to the application runtime when using those helpers. Caffeine and its JCache provider are API dependencies.

## First local cache {#quick-start}

Set capacity and expiry, then let the caller fill misses.

```kotlin
import io.bluetape4k.cache.caffeine.caffeine
import java.time.Duration

val users = caffeine {
    maximumSize(10_000)
    expireAfterWrite(Duration.ofMinutes(10))
    recordStats()
}.build<String, User>()

fun findUser(id: String): User {
    users.getIfPresent(id)?.let { return it }
    return userRepository.findById(id).also { loaded -> users.put(id, loaded) }
}
```

This is cache-aside. The cache does not update the repository, so the application must define how writes update or invalidate the cached value.

## API by task {#api-by-task}

| Task | Start with | Boundary to preserve |
| --- | --- | --- |
| Configure Caffeine | `caffeine`, `caffeineSpecOf`, `suspendLoadingCache` | Capacity, expiry, and stats are explicit builder choices. |
| Configure Cache2k | `cache2k`, `getOrCreateCache2k` | The application supplies the provider at runtime. |
| Configure Ehcache | `ehcacheManager`, `getOrCreateCache` | Managers enter `ShutdownQueue`; the helper includes a 32 MB off-heap tier. |
| Use JCache | `jcacheConfiguration`, `jcacheManager`, `JCaching` | Default expiry is eternal and provider managers are lazy singletons. |
| Use a coroutine cache | `SuspendJCache`, `CaffeineSuspendJCache` | Check whether the implementation uses non-blocking I/O or a blocking bridge. |
| Reuse function results | `Memoizer`, `AsyncMemoizer`, `SuspendMemoizer` | Test same-key merging, failure removal, and `clear()`. |
| Share two-tier contracts | `NearCacheOperations`, `SuspendNearCacheOperations` | Keys are `String`; provider implementations own invalidation. |
| Add retry and fallback | `withResilience`, `NearCacheResilienceConfig` | Read fallback and write failure policies differ. |

## Learning path {#concepts}

The chapters follow executable 1.12.1 source and tests rather than extending the README feature list. They explain defaults, concurrency, state after failures, and provider selection with short examples and direct source/test links.

1. [Local providers and JCache](./bluetape4k-cache-core/local-providers-jcache.md) — compare Caffeine, Cache2k, Ehcache, JCache managers, and expiry defaults.
2. [Cache-aside and loader/writer contracts](./bluetape4k-cache-core/cache-aside-loader-writer.md) — separate caller-owned loading from JCache read/write-through configuration.
3. [Memoizers and same-key computation](./bluetape4k-cache-core/memoizers-single-flight.md) — cover sync, future, and suspend computation, `SingleFlight`, failure, and `clear()`.
4. [Near Cache front/back semantics](./bluetape4k-cache-core/near-cache-semantics.md) — trace local hits, back lookups, front fills, invalidation, and statistics.
5. [Retry, failures, and lifecycle](./bluetape4k-cache-core/resilience-failures-lifecycle.md) — verify read fallback, write exceptions, cancellation, and close behavior.
6. [Testing and ecosystem paths](./bluetape4k-cache-core/testing-ecosystem-paths.md) — move from shared fixtures to Lettuce, Redisson, Exposed, and workshops.

Read 1→2→3 for local contracts, then 4→5 for distributed-cache boundaries. Existing provider users can start with the failure table in chapter 5 and the conformance fixtures in chapter 6.

## Patterns {#patterns}

Start local caches with bounded capacity and a short expiry. Measure evaluator count and eviction as well as hit rate. Do not cache failed loads. If concurrent misses can overload the backend, use same-key merging from a memoizer or a provider loading cache.

Document the order between source-data writes and cache invalidation. Do not label a cache-tier `put` as database write-through. Define the stale window when a source write succeeds but cache invalidation fails.

## Integrations {#integrations}

`bluetape4k-cache-lettuce` and `bluetape4k-cache-redisson` implement remote storage and invalidation on top of these common contracts. Continue with [bluetape4k-cache-lettuce](./bluetape4k-cache-lettuce.md) for Lettuce and RESP3, or [bluetape4k-cache-redisson](./bluetape4k-cache-redisson.md) for Redisson local cached maps.

Repository caching is a separate layer. Chapter 6 links `JdbcCacheRepository` and the Exposed workshops for real read-through, write-through, and write-behind examples.

## Configuration {#configuration}

There is no module-wide properties file. Configure capacity, expiry, and statistics through provider builders or JCache `MutableConfiguration`. `getDefaultJCacheConfiguration()` uses `EternalExpiryPolicy`. The Caffeine front cache in `NearJCacheConfig` expires 30 minutes after access by default; its remote synchronization timeout defaults to 500 ms.

Resilience defaults to three attempts, a 500 ms initial wait, exponential backoff, and `RETURN_FRONT_OR_NULL` for read failures. Attempts and wait duration must be positive.

## Failures {#failures}

`Cache.getOrPut` may run the supplier more than once during concurrent misses. `putIfAbsent` keeps the final cached value consistent, but it does not deduplicate external work. Use a same-key memoizer for expensive or side-effecting evaluation.

After retry exhaustion, read operations either return `null`, an empty map, or `false`, or propagate the exception according to `GetFailureStrategy`. Writes and removals propagate failure after retries. The suspend decorator always rethrows `CancellationException` without retrying or converting it to fallback data.

## Operations {#operations}

Track capacity, hits, misses, evictions, evaluator count, and load latency. For Near Caches, separate local and back-cache hits and misses, and chart remote cache errors with source-store latency and pool saturation.

Give each manager and cache one lifecycle owner. `CaffeineSuspendJCache.close()` invalidates local entries and runs cleanup. Ehcache and JCache provider helpers register managers with `ShutdownQueue`. Use unique cache names in tests and clear or close them afterward.

## Testing {#testing}

The module tests need no external Redis server.

```bash
./gradlew :bluetape4k-cache-core:test --no-build-cache --no-configuration-cache
```

`CaffeineSuspendJCacheTest` covers suspend CRUD. `SingleFlightTest` and provider memoizer tests cover concurrent evaluation, recovery after failure, and clear. The blocking and suspend resilience tests lock retry, fallback, cancellation, and close behavior. New providers can inherit the published fixtures to prove the same contract.

## Workshops {#workshops}

The module tests are the smallest runnable examples. Continue with the [cache-lettuce manual](./bluetape4k-cache-lettuce.md) and [cache-redisson manual](./bluetape4k-cache-redisson.md) for Redis invalidation.

For persistence strategies, use the cache chapters in [exposed-workshop](https://github.com/bluetape4k/exposed-workshop) and the examples in [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop). They introduce `JdbcCacheRepository`, `EntityMapLoader`, and `EntityMapWriter`, which distinguish a cache `put` from persistence write-through.

## 1.12.1 scope {#limitations}

This manual targets the `bluetape4k-projects` 1.12.1 release source. `cache-core` does not provide a Redis server, cluster invalidation, or a persistence transaction. Provider and repository modules own those concerns.

`SuspendJCache` is an asynchronous API shape, not a guarantee that every provider is non-blocking. Legacy `NearJCache` and the newer `NearCacheOperations` family also differ, so prefer the factory and shared fixture recommended by the selected provider manual.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### NearCache Interface Hierarchy diagram

[![NearCache Interface Hierarchy diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-01.svg)

_Release README: [`cache/cache-core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.md)_

### NearCacheOperations (Blocking) diagram

[![NearCacheOperations (Blocking) diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-02.svg)

_Release README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

### SuspendNearCacheOperations (Coroutine) diagram

[![SuspendNearCacheOperations (Coroutine) diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-03.svg)

_Release README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

### SuspendJCache coroutine interface diagram

[![SuspendJCache coroutine interface diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-04.svg)

_Release README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

### NearJCache synchronous two-tier cache diagram

[![NearJCache synchronous two-tier cache diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-05.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-05.svg)

_Release README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

### SuspendNearJCache coroutine operation map diagram

[![SuspendNearJCache coroutine operation map diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-06.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-06.svg)

_Release README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

### NearCache get() Sequence (front miss → back lookup → front fill) diagram

[![NearCache get() Sequence (front miss → back lookup → front fill) diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-sequence-01.svg)

_Release README: [`cache/cache-core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.md)_

### NearCache put() Sequence (write-through) diagram

[![NearCache put() Sequence (write-through) diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-sequence-02.svg)

_Release README: [`cache/cache-core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [`CaffeineSupport.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupport.kt)
- [`JCacheSupport.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheSupport.kt)
- [`CaffeineSuspendJCache.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/CaffeineSuspendJCache.kt)
- [`SingleFlight.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/memoizer/SingleFlight.kt)
- [`NearCacheOperations.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheOperations.kt)
- [`ResilientSuspendNearCacheDecorator.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecorator.kt)
- [`JCacheSupportExtTest.kt`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheSupportExtTest.kt)
- [`SingleFlightTest.kt`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/memoizer/SingleFlightTest.kt)
- [`ResilientSuspendNearCacheDecoratorTest.kt`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecoratorTest.kt)
