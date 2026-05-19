# Issue #493 Near-Cache Capability Matrix Design

## Context

Issue #493 asks for a documented near-cache backend capability matrix and a
shared conformance suite for Cache2k, Caffeine, Hazelcast, Lettuce, and Redisson
variants.

Current repository evidence:

- `cache-core` already owns shared test fixtures for `NearCacheOperations`,
  `SuspendNearCacheOperations`, `NearJCache`, and `SuspendNearJCache`.
- Lettuce, Hazelcast, and Redisson native near-cache implementations already
  inherit the operation-level fixtures.
- Lettuce, Redisson, Cache2k, and Ehcache JCache variants inherit the JCache
  fixtures.
- Hazelcast JCache listener registration is intentionally unsupported because
  Hazelcast distributes JCache listener configuration through serialization and
  the current listener captures non-serializable front-cache state.

## Scope

This change documents and tests the existing support boundary instead of
introducing a new backend abstraction.

In scope:

- Add a docs-level capability matrix for native near-cache and JCache near-cache
  variants, with explicit sync and suspend axes.
- Update cache module README pairs with the matrix or a link to the matrix.
- Strengthen shared operation conformance tests for write propagation after
  local eviction.
- Replace disabled Hazelcast JCache tests with explicit unsupported/degraded
  behavior tests.
- Remove silent conformance skips where a backend has an unsupported operation.
- Mark Caffeine and Cache2k local-only/provider-only combinations explicitly in
  the matrix instead of implying distributed near-cache support.

Out of scope:

- Changing Hazelcast JCache listener serialization.
- Adding distributed invalidation to local-only Caffeine or Cache2k providers.
- Changing public cache API names.

## Capability Model

Capability states:

- `Supported`: behavior is implemented and covered by shared conformance tests.
- `Factory degraded`: factory intentionally creates a near cache without listener
  registration; write-through and read-through work, but peer front-cache
  propagation is not promised.
- `Local only`: backend can be used as a local/front or local JCache provider,
  but does not provide distributed back-cache invalidation by itself.
- `Unsupported`: direct listener-backed construction is expected to fail or is
  not part of the public contract.

## Acceptance Mapping

- Public matrix: `docs/cache/near-cache-capability-matrix.md` plus README links.
- Same conformance suite: direct supported backends continue to inherit
  `AbstractNearCacheOperationsTest` and `AbstractSuspendNearCacheOperationsTest`;
  JCache variants continue to inherit `AbstractNearJCacheTest` and
  `AbstractSuspendNearJCacheTest` where supported.
- Unsupported combinations: Hazelcast JCache direct listener registration is
  tested as unsupported; Hazelcast factories are tested as listener-free
  degraded support; Cache2k JCache whole-cache `removeAll()` propagation is
  tested as explicitly unsupported; Caffeine and Cache2k local-only combinations
  are documented as exclusions from distributed near-cache behavior.
- Disabled tests: disabled Hazelcast JCache test classes are replaced by active
  explicit behavior tests.

## Risks

- Container-backed cache tests can be slow and must run serially.
- Existing Cache2k `removeAll()` behavior is not equivalent to distributed
  backends; the conformance override must assert this explicitly instead of
  hiding it.
