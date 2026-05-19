# Near-Cache Backend Capability Matrix

This matrix defines the supported near-cache behavior for cache modules. It is a
runtime support boundary, not just a test inventory.

Capability states:

- `Supported`: implemented and covered by the shared conformance fixtures.
- `Factory degraded`: factory methods intentionally avoid listener
  registration; read-through and write-through work, but peer front-cache
  propagation is not promised.
- `Local only`: usable as a local/front cache or local JCache provider; it does
  not provide distributed back-cache invalidation by itself.
- `Unsupported`: direct construction is expected to fail or is not part of the
  public contract.

## Native NearCache APIs

Native APIs implement `NearCacheOperations<V>` or
`SuspendNearCacheOperations<V>`.

| Backend | Sync near-cache | Suspend near-cache | Listener or invalidation source | put/replace/remove propagation | removeAll semantics | Back cache scope | Conformance |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Lettuce | Supported | Supported | Redis RESP3 `CLIENT TRACKING` plus explicit write-through | Supported | Removes selected keys from front and Redis back cache | Distributed Redis | `AbstractNearCacheOperationsTest`, `AbstractSuspendNearCacheOperationsTest` |
| Hazelcast IMap | Supported | Supported | Hazelcast `IMap` entry listener | Supported | Removes selected keys from front and IMap back cache | Distributed Hazelcast map | `AbstractNearCacheOperationsTest`, `AbstractSuspendNearCacheOperationsTest` |
| Redisson `RLocalCachedMap` | Supported | Supported | Redisson local cached map invalidation | Supported | Removes selected keys from local and Redis-backed map | Distributed Redis via Redisson | `AbstractNearCacheOperationsTest`, `AbstractSuspendNearCacheOperationsTest` |
| Caffeine | Local only | Local only | Local cache events only | Local only | Local only | Local JVM | Covered as front/local cache, not native distributed near-cache |
| Cache2k | Local only | Not provided as native suspend near-cache | Local cache events only | Local only | Local only | Local JVM | Covered as local/JCache provider, not native distributed near-cache |

## JCache NearCache APIs

JCache APIs implement `NearJCache<K,V>` or `SuspendNearJCache<K,V>`.

| Back provider | Sync `NearJCache` | Suspend `SuspendNearJCache` | Listener registration | Peer front propagation | removeAll semantics | Back cache scope | Conformance |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Lettuce JCache | Supported | Supported | Supported | Supported | Supported through per-entry removal where needed | Distributed Redis | `AbstractNearJCacheTest`, `AbstractSuspendNearJCacheTest` |
| Redisson JCache | Supported | Supported | Supported | Supported | Supported through per-entry removal where needed | Distributed Redis via Redisson | `AbstractNearJCacheTest`, `AbstractSuspendNearJCacheTest` |
| Hazelcast JCache direct listener construction | Unsupported | Unsupported | Unsupported: listener configuration must be serializable for cluster distribution | Unsupported | Unsupported in listener-backed direct construction | Distributed Hazelcast JCache | Explicit unsupported tests in `cache-hazelcast` |
| Hazelcast factory methods | Factory degraded | Factory degraded | Not registered intentionally | Not promised | `clearAllCache` / `clearAll` clear front and back for the local wrapper; listener propagation is not promised | Distributed Hazelcast JCache | Explicit factory behavior tests in `cache-hazelcast` |
| Cache2k JCache | Supported except whole-cache `removeAll()` propagation | Not provided | Local provider listener | Same-JVM only | Whole-cache `removeAll()` propagation is explicitly unsupported by conformance test | Local JVM | `AbstractNearJCacheTest` with explicit unsupported override |
| Caffeine JCache/front | Local only | Supported as front `SuspendJCache` | Local provider listener | Same-JVM only | Local/front only unless paired with a distributed back cache | Local JVM | Used as front cache in suspend JCache conformance |
| Ehcache JCache | Supported | Not provided in this suite | Local provider listener | Same-JVM only | Supported by the sync JCache fixture | Local JVM | `AbstractNearJCacheTest` |

## Test Ownership

- `cache-core/src/testFixtures/.../AbstractNearCacheOperationsTest.kt` and
  `AbstractSuspendNearCacheOperationsTest.kt` define native near-cache
  conformance.
- `cache-core/src/testFixtures/.../jcache/AbstractNearJCacheTest.kt` and
  `AbstractSuspendNearJCacheTest.kt` define JCache near-cache conformance.
- Backend modules inherit these fixtures for supported combinations.
- Unsupported combinations must use active tests that assert the unsupported or
  degraded behavior; disabled test classes are not an acceptable support marker.
