---
title: RESP3 invalidation and Lua CAS
description: Configure CLIENT TRACKING, process push payloads, understand NOLOOP, and use EVALSHA with NOSCRIPT fallback.
manualId: bluetape4k-cache-lettuce
chapterId: resp3-invalidation-lua
---

# RESP3 invalidation and Lua CAS

## Configure the client for RESP3

Redis CLIENT TRACKING sends an invalidation push when another connection changes a key read by this connection. Lettuce receives those pushes over RESP3.

```kotlin
val redisClient = RedisClient.create("redis://localhost:6379").also {
    it.options = ClientOptions.builder()
        .protocolVersion(ProtocolVersion.RESP3)
        .build()
}
```

`useRespProtocol3=true` tells the near cache to request `CLIENT TRACKING ON NOLOOP`; it does not change the protocol on the `RedisClient` itself.

## Register read keys

An L1 miss performs Redis GET and registers the key for tracking. Successful `put`, `putAll`, `putIfAbsent`, and `replace` paths perform a synchronous GET or MGET after the write.

This read establishes command order. A fire-and-forget GET could reach Redis after an external update, missing the invalidation window. The synchronous read returns before the write API finishes.

## NOLOOP and self writes

`NOLOOP` suppresses pushes for writes on the same connection. The near cache already updates its own L1 after success. Another cache instance or Redis connection changing the prefixed key produces the push.

```text
Cache A GET users:42 -> tracking and L1 fill
Cache B SET users:42 -> invalidation push to A
Cache A listener -> invalidate("42")
Cache A next GET -> fill with the new Redis value
```

Push delivery is asynchronous. Cache B returning from a write does not prove that Cache A has already invalidated L1.

## Process push payloads

`TrackingInvalidationListener` accepts key payloads as `ByteBuffer`, `ByteArray`, String, or a list. It removes the `${cacheName}:` prefix and invalidates only matching keys. A null key list means full invalidation and clears that L1.

Decode failures are logged and skipped. Tests cover mixed payloads, single keys, foreign prefixes, and full flush.

## Tracking startup is fail-open

If `trackingListener.start()` fails, construction logs a warning and continues. Redis reads, writes, and L1 fill still work, but remote changes can leave stale L1 entries.

A Redis ping is not enough for health verification. Check RESP3 protocol, CLIENT TRACKING permissions, and a real cross-instance invalidation. If tracking is unavailable, shorten L1 TTL or disable it deliberately and provide another invalidation path.

## Lua compare-and-set

`replace(key, oldValue, newValue)` atomically compares and replaces with Lua. The script uses `SET ... XX KEEPTTL`, so a successful replacement preserves the existing TTL.

The client first sends `EVALSHA` with the precomputed SHA1. Only `RedisNoScriptException` triggers fallback to the source through `EVAL`, allowing recovery after script cache flush or failover. Connection, timeout, and codec failures are not hidden by a raw-script retry.

## Verification scenarios

- cross-instance updates and removes under one cache name
- isolation for the same logical key under another cache name
- tracking after bulk, put-if-absent, and replacement writes
- null, mixed, and single-key push payloads
- CAS after script-cache flush with TTL preservation
- stale-read bounds when tracking startup fails

## Sources and tests

- [`TrackingInvalidationListener.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/TrackingInvalidationListener.kt)
- [`NearCacheScripts.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheScripts.kt)
- [`LettuceNearCache.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt)
- [`LettuceNearCacheTrackingTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheTrackingTest.kt)
- [`TrackingInvalidationListenerPayloadTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/TrackingInvalidationListenerPayloadTest.kt)
