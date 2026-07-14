---
title: Cache-aside and loader/writer contracts
description: Separate caller-owned cache-aside from JCache loader and writer contracts for read-through and write-through.
manualId: bluetape4k-cache-core
chapterId: cache-aside-loader-writer
---

# Cache-aside and loader/writer contracts

## The caller owns cache-aside

With cache-aside, the service or repository observes a miss, reads source data, and puts the result into the cache.

```kotlin
fun findProduct(id: Long): Product {
    productCache.get(id)?.let { return it }
    return productRepository.find(id).also { productCache.put(id, it) }
}
```

The first request reads the database. The cache API does not know the source store, so the caller owns update and invalidation after a write transaction.

## Concurrent misses in `getOrPut`

The JCache `getOrPut` extension gets the key, evaluates a supplier on a miss, and calls `putIfAbsent`. Concurrent misses may evaluate the supplier more than once. The final return value is aligned with the first cached value, but external work is not deduplicated.

```kotlin
val product = cache.getOrPut(id) { productRepository.find(id) }
```

Use a same-key memoizer or a provider loading cache when duplicate database work is not acceptable.

## JCache read-through

JCache read-through exists when a `CacheLoader` factory is registered with `isReadThrough=true`. The `cacheLoader()` extension in cache-core merely adapts another cache; supply a repository-backed loader for database reads.

```kotlin
val config = jcacheConfigurationOf<Long, Product>(
    cacheLoaderFactory = FactoryBuilder.factoryOf(ProductLoader(productRepository)),
    isReadThrough = true,
)
```

Test provider exception wrapping and null handling because the loader now owns the miss path.

## The exact scope of write-through

JCache write-through requires a `CacheWriter` factory and `isWriteThrough=true`, with cache changes invoking the backing store. The `cache.cacheWriter()` helper delegates to that cache's own `put` and `remove`; it is not a database writer.

A Near Cache `put` similarly updates local and remote cache tiers. It does not update a database or Exposed table, so it is not persistence write-through.

## Partial failure around source data

Writing source data first can leave a stale entry when invalidation fails. Writing the cache first can expose data that never committed. Without a transaction across both systems, choose an explicit rule:

- write source data, then invalidate, and bound stale time with a short TTL;
- retry invalidation through an outbox or event and measure delay;
- use a repository design such as `JdbcCacheRepository` that owns loader, writer, and persistence boundaries.

## Sources and tests

- [`JCacheSupport.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheSupport.kt)
- [`JCacheReadWriteThroughExample.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheReadWriteThroughExample.kt)
- [`JCacheSupportExtTest.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheSupportExtTest.kt)
- [`JCacheMemoizer.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/memoizer/jcache/JCacheMemoizer.kt)
