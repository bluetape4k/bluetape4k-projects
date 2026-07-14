---
title: Operations and ecosystem
description: Observe Lettuce in production and choose the next cache, Hibernate, Exposed, or workshop layer.
manualId: bluetape4k-lettuce
chapterId: operations-and-ecosystem
---

# Operations and ecosystem

## What to observe

Track Redis connection and reconnect state, command latency and timeouts, pipeline batch size, and codec decode failures. Loaded maps add hit/miss rates, loader latency, write-behind queue depth, retries, dead letters, and shutdown draining. Queue saturation usually means the writer cannot keep up with Redis writes, not merely that the configured capacity is small.

## From tests to production failures

`LettuceClientsTest` covers connection reuse and multi-thread, virtual-thread, and coroutine access. `RedisFutureSupportTest` checks ordering and failure propagation. Loaded-map tests cover writer failures, dead letters, and caller-scope ownership. Benchmark numbers describe one machine and payload set; they are not an application SLA.

## Choose the next layer

- Stay here for direct Redis commands and coroutine adapters.
- Use [`bluetape4k-cache-lettuce`](../bluetape4k-cache-lettuce.md) for memoization and cache abstractions.
- Use [`bluetape4k-hibernate-cache-lettuce`](../bluetape4k-hibernate-cache-lettuce.md) for Hibernate second-level caching.
- For database-backed loading and writing, define transaction boundaries with [`bluetape4k-jdbc`](../bluetape4k-jdbc.md), [`bluetape4k-hibernate`](../bluetape4k-hibernate.md), or Exposed repositories.
- Continue with [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop) and [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop) for complete examples.

Do not label cache-aside PUT management as true write-through. A write-through or write-behind example must include the database writer and its consistency boundary.

## Source and tests

- [`Benchmark.md`](../../../../../infra/lettuce/Benchmark.md)
- [`LettuceSuspendedLoadedMapTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/map/LettuceSuspendedLoadedMapTest.kt)
- [`FastForyCompatibilityTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/FastForyCompatibilityTest.kt)
