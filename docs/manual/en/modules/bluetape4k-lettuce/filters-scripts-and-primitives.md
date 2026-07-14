---
title: Filters, scripts, and primitives
description: Select probabilistic structures, Lua fallback, and Redis-backed atomic wrappers.
manualId: bluetape4k-lettuce
chapterId: filters-scripts-and-primitives
---

# Filters, scripts, and primitives

## Read probabilistic results correctly

For a Bloom filter, `contains=false` proves absence while `true` may be a false positive. A Cuckoo filter supports deletion, but insertion can fail in a constrained bucket layout. Reinitializing an existing filter with different parameters raises an exception.

```kotlin
val filter = LettuceBloomFilter(
    connection,
    "blocked-email",
    BloomFilterOptions(expectedInsertions = 100_000, falseProbability = 0.01),
)
filter.tryInit()
filter.add("spam@example.com")
```

HyperLogLog provides approximate cardinality, not an exact set size, and is unsuitable for billing or hard quota decisions.

## Lua fallback

`RedisScript` precomputes the source SHA1. Every `RedisScriptRunner` mode tries `EVALSHA` first and sends the source with `EVAL` only after `NOSCRIPT`. Other script failures propagate.

AtomicLong, semaphore, and lock wrappers combine Redis commands with Lua. Release locks and permits in `finally`, and do not equate a Redis mutex with business atomicity under network partitions.

## Source and tests

- [`RedisScript.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/script/RedisScript.kt)
- [`LettuceBloomFilter.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/filter/LettuceBloomFilter.kt)
- [`LettuceCuckooFilter.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/filter/LettuceCuckooFilter.kt)
- [`RedisScriptTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/script/RedisScriptTest.kt)

Continue with [Operations and ecosystem](./operations-and-ecosystem.md).
