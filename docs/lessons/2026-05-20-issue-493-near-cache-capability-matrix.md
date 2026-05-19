# Issue #493 Near-Cache Capability Matrix

## Context

Issue #493 required a documented backend capability matrix and inherited
conformance tests for near-cache behavior across local and distributed cache
providers.

## Decision

Keep the support boundary documentation-first and fixture-based:

- Native Lettuce, Hazelcast IMap, and Redisson near caches keep sharing the
  `NearCacheOperations` / `SuspendNearCacheOperations` fixtures.
- JCache-backed Lettuce and Redisson near caches keep sharing the JCache
  fixtures.
- Hazelcast JCache listener-backed construction is unsupported because listener
  configuration must be serializable for cluster distribution.
- Hazelcast factories remain listener-free degraded support for read-through and
  write-through behavior.
- Cache2k whole-cache JCache `removeAll()` propagation is an explicit
  unsupported conformance case, not a silent skip.

## Outcome

Added `docs/cache/near-cache-capability-matrix.md`, linked it from cache module
README pairs, strengthened shared conformance tests, and replaced disabled
Hazelcast JCache tests with active unsupported/degraded behavior tests.

## Verification

Ran the targeted downstream conformance suite:

```text
./gradlew :bluetape4k-cache-core:test ... :bluetape4k-cache-hazelcast:test ... :bluetape4k-cache-lettuce:test ... :bluetape4k-cache-redisson:test ... --console=plain --no-configuration-cache
BUILD SUCCESSFUL
cache-core: 134 tests
cache-hazelcast: 47 tests
cache-lettuce: 184 tests
cache-redisson: 244 tests
```

Also verified `@Disabled` is absent from the near-cache test scope and
`git diff --check` passes.

## Future Guidance

For near-cache capability changes, update the matrix first, then require each
supported backend to inherit the same fixture. Unsupported combinations should
be active tests or explicit matrix exclusions, never disabled abstract tests.

