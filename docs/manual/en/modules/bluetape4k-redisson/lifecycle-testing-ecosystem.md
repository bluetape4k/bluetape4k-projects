---
title: Lifecycle, testing, and ecosystem path
description: Operate Redisson resources, verify them with Testcontainers, and progress to cache-redisson, Exposed, and workshops.
manualId: bluetape4k-redisson
chapterId: lifecycle-testing-ecosystem
---

# Lifecycle, testing, and ecosystem path

## Choose the required layer

| Required level | Choice | Responsibility |
| --- | --- | --- |
| Direct Redisson objects | `bluetape4k-redisson` | Clients, codecs, batch/transaction, streams, coroutines, local cached maps |
| Spring Cache annotations | [`bluetape4k-cache-redisson`](../bluetape4k-cache-redisson.md) | `CacheManager`, regions, and Spring Cache integration |
| Database repository cache | [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed) | Loader/writer-backed entity repositories |
| Guided practice | [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop) | Cache-aside, read/write-through, Near Cache, and measurement |

Higher layers still depend on client, codec, invalidation, and shutdown behavior from this module.

## Startup and shutdown order

1. Fix configuration and codec.
2. Create one client and pass its health check.
3. Attach maps, listeners, and near-cache references.
4. Stop new work and drain write-behind and consumers during shutdown.
5. Destroy near-cache instances, then shut down the client.

Let Spring own a managed bean. Close directly created clients in production and tests to avoid leaking threads and connections.

## Operational signals

Observe connected nodes and pool use, command percentiles and timeout, retry and reconnect, batch size, rollback, Stream pending age and claims, local-cache hit and invalidation rates, stale incidents, writer backlog and drain time, codec failures, and payload size.

A high hit ratio is not healthy when stale-data incidents also rise. Measure freshness against the source of truth.

## Reading the 1.12.1 tests

`AbstractRedissonTest` provides the Redis Testcontainer and shared fixture. Continue through client configuration, batch/transaction, future/coroutine, local-cache, configuration validation, and codec tests in that order.

```bash
./gradlew :bluetape4k-redisson:test --no-build-cache --no-configuration-cache
```

This is a Docker-backed Testcontainers task. Serialize it with other database and Redis suites. Add Redis restart, Pub/Sub disconnect, latency injection, and process shutdown to application-level failure tests.

## Workshop checks

Trace data flow before API names: whether the first request reads the database and fills cache, whether misses call a loader, and whether writes pass through a writer. Benchmark reports should include environment, payload, metric direction, and allowed staleness rather than copying headline numbers.

## Related paths

- [`bluetape4k-cache-redisson`](../bluetape4k-cache-redisson.md)
- [`bluetape4k-coroutines`](../bluetape4k-coroutines.md)
- [`bluetape4k-testcontainers`](../bluetape4k-testcontainers.md)
- [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)
- [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)
- [bluetape4k Workshop](https://github.com/bluetape4k/bluetape4k-workshop)
- [bluetape4k ecosystem atlas](https://bluetape4k.github.io/ecosystem/atlas/)
