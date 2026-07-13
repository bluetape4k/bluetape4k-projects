---
title: Keys, concurrency, and invalidation
description: Understand Hibernate key digests, composite and natural IDs, concurrency strategies, and RESP3 invalidation.
manualId: bluetape4k-hibernate-cache-lettuce
chapterId: keys-concurrency-invalidation
---

# Keys, concurrency, and invalidation

## Do not use `toString()` as a key

Hibernate keys contain the entity or role name, tenant, identifier kind, and value. StorageAccess writes a canonical byte sequence, hashes it with SHA-256, and prefixes the digest with `hck2:`.

```text
kind + entityOrRoleName + tenant presence/value + typed identifier
  → SHA-256
  → hck2:<base64url digest>
```

A scalar `"[1, 2]"` and an object array `[1, 2]` remain distinct, as do a delimiter-containing natural ID and a multi-value natural ID. Tests also cover composite IDs, primitive arrays, and custom IDs with equal `toString()` output.

## Match strategy to the data contract

The factory default is `NONSTRICT_READ_WRITE`. Updates evict entries and later reads refill them instead of coordinating a strict distributed soft lock. It fits read-heavy data that permits a short stale window.

The suite also covers `READ_WRITE`, but its Redis soft-lock costs and failure behavior need measurement. A lock marker may remain after delete, so the test explicitly evicts before checking containment. Reserve `READ_ONLY` for immutable reference data.

## RESP3 CLIENT TRACKING

With `use_resp3=true`, the factory selects RESP3 and Near Cache starts a tracking listener. A change through another Redis connection should push invalidation to the corresponding L1 key.

Tracking startup failure is warning-only:

```text
CLIENT TRACKING start failed, cache will work without invalidation
```

Until TTL or Hibernate eviction, another process may then retain a stale L1 value. Monitor startup logs and run a multi-instance invalidation test. Setting `use_resp3=false` for Redis older than 6 gives up this cross-process path.

## Avoid direct Redis writes

CLIENT TRACKING does not make Hibernate region keys a public mutation API. The versioned key format, query timestamps, and transaction-completion rules are internal. Route changes through Hibernate and `SessionFactory.cache.evict*`.

## Sources and tests

- [`LettuceNearCacheStorageAccess.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt)
- [`HibernateAdvancedKeyCacheTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateAdvancedKeyCacheTest.kt)
- [`HibernateReadWriteStrategyTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateReadWriteStrategyTest.kt)
- [`LettuceNearCacheTrackingTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheTrackingTest.kt)
