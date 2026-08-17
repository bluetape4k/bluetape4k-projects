# Module bluetape4k-cache-core

English | [한국어](./README.ko.md)

`bluetape4k-cache-core` provides the shared cache API, core abstractions, and **local cache implementations**.

> The former `bluetape4k-cache-local` module was merged into this module.

## Package / Import Stability

The cache folder reorganization moved this module under `cache/cache-core/`, but
the Gradle project name, Maven artifact ID, and Kotlin packages remain stable:

- Gradle project: `:bluetape4k-cache-core`
- Maven artifact: `io.github.bluetape4k:bluetape4k-cache-core`
- Kotlin package root: `io.bluetape4k.cache`

No user import migration is required for the reorganization.

## Provided Features

- **Common JCache utilities**: `JCaching`, `jcacheManager`, `jcacheConfiguration`, and more
- **Coroutines cache abstractions**: `SuspendCache`, `SuspendCacheEntry`
- **Unified NearCache interfaces**: `NearCacheOperations<V>`, `SuspendNearCacheOperations<V>`, `NearCacheStatistics`
- **Resilient decorators**: `ResilientNearCacheDecorator`, `ResilientSuspendNearCacheDecorator`
- **JCache NearCache**: `NearJCache<K,V>`, `SuspendNearJCache<K,V>`
- **Memoizer abstractions** for sync, async, and suspend flows
- **Local cache providers**: Caffeine, Cache2k, and Ehcache

## Architecture Diagrams

### NearCache get() Sequence (front miss → back lookup → front fill)

![NearCache get() Sequence (front miss → back lookup → front fill) diagram](../../docs/images/readme-diagrams/cache-cache-core-sequence-01.png)

### NearCache put() Sequence (write-through)

![NearCache put() Sequence (write-through) diagram](../../docs/images/readme-diagrams/cache-cache-core-sequence-02.png)

### NearCache Interface Hierarchy

![NearCache Interface Hierarchy diagram](../../docs/images/readme-diagrams/cache-cache-core-diagram-01.png)

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-core:${bluetape4kVersion}")
}
```

Add the appropriate provider module if you need distributed caching.

## Detailed Features

### JCache NearCache contract

`NearJCache<K,V>` is a logical two-tier implementation of `javax.cache.Cache`.
Standard `get`, `containsKey`, and `getAll` check the front cache first, fall
back to the back cache on a miss, and return every hit from both tiers. Front
residency for bulk back hits follows the configured policy described below.
Standard `clear` removes mappings from both layers owned by the wrapper. The
legacy constructors and provider factories use
`NearJCacheClearAuthority.DENY`, so `clear()`, `clearAllCache()`, and no-argument
`removeAll()` fail with `SecurityException` before mutating either tier. A caller
that has verified exclusive ownership of the back namespace must opt in with
`NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE`; key-scoped `removeAll(keys)` and
single-key `remove` remain available without that authority. The authority is
runtime-only and is not part of `NearJCacheConfig` serialization. A direct
`nearCache.backCache.clear()` call is a caller-owned escape hatch outside this
wrapper guard, so do not pass the back-cache reference to untrusted code. The
`ResilientNearJCache` and `ResilientSuspendNearJCache` `ClearBack` paths are not
covered by this contract. The `getDeeply` and `clearAllCache` names remain
source-compatible aliases for the standard `get` and `clear` behavior.

<!-- issue-1369-bulk-policy:start -->
### Bulk `getAll` front residency policy

<!-- contract: default-bypass; bounded-all-or-nothing; single-key-get-unchanged; repeated-back-read; legacy-safe-default -->

```kotlin
val safeDefault = NearJCacheConfig<String, User>()
val bounded = NearJCacheConfig<String, User>(
    bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(128),
)
```

The default `BulkFrontPopulationPolicy.BypassFront` still returns front hits and
all back hits, but it does not store the bulk back result in front. With
`BulkFrontPopulationPolicy.PopulateIfAtMost(n)`, the whole batch is stored only
when `backValues.size <= n`; an oversized batch is not partially stored. The
entry count is neither resident byte size nor a limit on the back query size.
Single-key `get()` read-through population is unchanged.

The configuration MXBean reports `BYPASS_FRONT` or `POPULATE_IF_AT_MOST` and
`bulkFrontPopulationMaximumEntryCount`; `0` means the limit does not apply to
the bypass policy. New configuration and a restored legacy stream both use the
safe bypass default. Results remain correct, but repeated `getAll` calls can
perform repeated back reads and change local hit ratio and back load. Do not
restore the former unlimited batch-population mode. Opt in to a bounded policy
only after checking front capacity and the local heap budget.
<!-- issue-1369-bulk-policy:end -->

The clear operation does not promise invalidation of another wrapper's already
populated front cache when the back cache is shared or listener propagation is
unavailable. Use the existing per-entry `removeAll` path when peer invalidation
is required. `getAndPut`, `getAndRemove`, and `getAndReplace` use the back
provider's atomic compound operation and then reconcile the local front cache;
they do not implement a front-read/back-write round trip. A provider failure is
propagated before the front cache is changed.

Use `nearCache.lastBackCacheWrite` when correlating the latest write-through
operation ID, operation name, and completion. It is one atomic observation
snapshot, and its completion stage is read-only. The legacy
`lastBackCacheWriteOperationId` and `lastBackCacheWriteCompletion` properties
remain for source compatibility, but separate reads are not a correlated
snapshot.

```kotlin
val observation = nearCache.lastBackCacheWrite
observation.completion.toCompletableFuture().join()
```

When `NearJCacheConfig.isSynchronous=true`, synchronous write-through runs the
blocking provider call on a dedicated virtual thread and waits only up to the
bounded `syncRemoteTimeout` (with a 500 ms minimum). If a provider invokes the
listener for that same write inline or on a synchronous callback thread,
`NearJCache` uses an operation-scoped key/type/value match to reconcile the
self-event directly to the front cache instead of reacquiring `mutationGate`;
this prevents self-deadlock. Non-matching peer or external events still use
`mutationGate`, and asynchronous write-through keeps the existing gated path.
JCache events do not carry an operation ID, so an external event with the same
key/type/value cannot be distinguished from the active self-event. A provider that
ignores interruption may complete after the caller observes a timeout;
`backWriteLock` serializes that late completion with following writes.

`SuspendNearJCache` uses the same back-first rule for ordinary mutations:
`put`, `putAll`, `putIfAbsent`, `remove`, and `replace` update the back cache
before reconciling the local front cache. A back failure leaves the front
unchanged; if front reconciliation fails after a back commit, the affected
front key(s) are invalidated so an uncommitted value is not returned. Coroutine
cancellation is propagated without fallback or retry.

The default front configuration uses store-by-reference. A custom store-by-value
front configuration is rejected until a filtered, provider-specific copier
contract is supplied.

`close()` closes explicitly registered MXBean handles, deregisters the listener
registered by this wrapper, and closes only its owned front cache; it never
closes the supplied back cache, `MBeanServer`, cache manager, or provider. Cleanup
failures are observable: the first failure is propagated and later cleanup
failures are attached as suppressed exceptions. A successful `close()` is
idempotent, while a listener-deregistration or front-close failure is retried by
the next call. Listener registration is rejected once close has started. If
construction fails after the front cache is created, the same
primary/suppressed exception policy is applied during rollback.

<!-- issue-1351-nearcache-management:start -->
### Explicit NearJCache management and statistics

Management is opt-in. Configure the cache types and both feature flags before
constructing the wrapper, keep `storeByValue` disabled for the front cache, and
register the MXBeans in a caller-owned `MBeanServer` with stable, non-secret
IDs. The IDs become part of `ObjectName`; do not use credentials, tokens, or
personal data.

```kotlin
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthority
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheConfigurationMXBean
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheTierStatisticsMXBean
import io.bluetape4k.cache.nearcache.jcache.management.registerMBeans
import java.lang.management.ManagementFactory
import javax.cache.configuration.MutableConfiguration
import javax.management.JMX

val manager = NearJCacheConfig.CaffeineCacheManagerFactory.create()
val configuration = MutableConfiguration<String, String>()
    .setTypes(String::class.java, String::class.java)
    .setStatisticsEnabled(true)
    .setManagementEnabled(true)
    .setStoreByValue(false)
val front = manager.createCache("orders-front", configuration)
val back = manager.createCache("orders-back", configuration)
val nearCache = NearJCache(
    front,
    back,
    NearJCacheConfig(frontCacheConfiguration = configuration),
    NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
)
val server = ManagementFactory.getPlatformMBeanServer()
val registration = nearCache.registerMBeans(server, "orders-service", "orders-v1")
val names = registration.activeObjectNames.associateBy { it.getKeyProperty("type") }
val management = JMX.newMXBeanProxy(server, names.getValue("NearJCacheConfiguration"), NearJCacheConfigurationMXBean::class.java)
val statistics = JMX.newMXBeanProxy(server, names.getValue("NearJCacheStatistics"), NearJCacheTierStatisticsMXBean::class.java)

statistics.clear() // resets only logical/tier counters
nearCache.clear()  // removes data from this wrapper's front and back caches
registration.close()
nearCache.close()
back.close()
```

The factory returns a provider-managed cache manager. Close that manager only
as part of the application's provider shutdown, not as wrapper cleanup.

Java callers use
`NearJCacheMBeans.registerMBeans(nearCache, server, managerId, cacheId)`.
The immutable management snapshot reports the configuration used at wrapper
construction. Statistics use `statisticsScope=NEAR_JCACHE_WRAPPER_V1`; inspect
`supportedOperations` before interpreting a counter. The capability getters
`isFrontEvictionObservationSupported`, `isBulkRemovalCountSupported`, and
`isBackWriteCompletionIncluded` currently return `false`. A `false` capability
means “not observed,” not “the event never happened.”

For asynchronous write-through, a successful API return counts caller-visible
acceptance only. Track each `BackCacheWriteCompletion` by the stable
`operationId` and its diagnostic `operation` name until remote completion.
There is no zero-loss global drain API. Before migration, stop new admission
and keep an application-owned inventory of outstanding completions. For a
synchronous handover, close the old registration handle, close the old
`nearCache`, and only then create/register the replacement.

Registration owns only the exact MXBean names returned by its handle. It never
owns the `MBeanServer`, back cache, cache manager, or provider. Keep that
namespace exclusive while the handle is active. On a collision, inspect the
existing owner before retrying. Treat `RECOVERY_REQUIRED` as an immediate
cleanup incident and retry `close()` on the recovery handle; the ownership
token is a best-effort stale-owner defense, not an atomic JMX compare-and-swap.
See the [operations guide](../../docs/operations/issue-1351-nearcache-management.md)
for rollout classification and cleanup evidence.
<!-- issue-1351-nearcache-management:end -->

<!-- nearjcache-clear-authority-contract -->
### #1368 shared-back clear authority

The default wrapper is safe for a shared back namespace. Use key-scoped removal
for a tenant-owned key set; only an explicitly verified exclusive owner may use
namespace-wide clear operations. The enum is runtime-only and does not change
the serializable `NearJCacheConfig`.

```kotlin
val shared = NearJCache(front, back, NearJCacheConfig(), NearJCacheClearAuthority.DENY)
shared.removeAll(setOf("tenant-a:key-1"))
val owner = NearJCache(
    front,
    back,
    NearJCacheConfig(),
    NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
)
owner.clearAllCache()
```
<!-- /nearjcache-clear-authority-contract -->

## Near-Cache Capability Matrix

The shared support boundary for native and JCache near-cache variants is tracked
in [Near-Cache Backend Capability Matrix](../../docs/cache/near-cache-capability-matrix.md).

- Lettuce, Hazelcast IMap, and Redisson native near caches are covered by the
  shared `NearCacheOperations` / `SuspendNearCacheOperations` conformance
  fixtures.
- Lettuce and Redisson JCache near caches are listener-backed and covered by the
  shared JCache conformance fixtures.
- Hazelcast JCache factory methods are listener-free by design; direct
  listener-backed construction is unsupported and tested explicitly.
- Caffeine and Cache2k are local providers unless paired with a distributed back
  cache through a supported near-cache implementation.

### Unified NearCache Interface

All NearCache backends, including Lettuce, Hazelcast, Redisson, and JCache-based implementations, share a common interface.

- `NearCacheOperations` is the blocking contract.
- `SuspendNearCacheOperations` is the coroutine contract.
- `NearCacheStatistics` exposes hit/miss and capacity-oriented counters.
- Resilience decorators wrap these interfaces to add retry and failure strategies.

The Korean README contains the full sequence diagrams and class diagrams for `get()`,
`put()`, and JCache-backed two-tier caches.

## Basic Usage Examples

Typical usage patterns:

- local cache only through Caffeine / Cache2k / Ehcache providers
- common cache abstractions shared across distributed backends
- resilience decorators in front of remote NearCache implementations
- memoizers for repeatable, computation-heavy functions

### Suspend Memoizer Failure Recovery

Suspend memoizers merge concurrent calls for the same key through an in-flight
`Deferred`. If the evaluator fails or the caller is cancelled, that in-flight
entry is removed so a later call can recompute instead of replaying a stale
failure.

```kotlin
var attempts = 0
val memo = suspendMemoizer<String, Int> { key ->
    attempts += 1
    if (attempts == 1) error("temporary backend failure")
    key.length
}

runCatching { memo("recover") }  // fails once
val value = memo("recover")      // recomputes and returns 7
```

## Recommended Usage Patterns

- Use `cache-core` directly when local cache and common abstractions are enough.
- Use provider modules such as Hazelcast, Lettuce, or Redisson when remote storage or invalidation is required.
- Prefer the newer `Memoizer` / `AsyncMemoizer` / `SuspendMemoizer` abstractions for new code.
- Use `NearCacheOperations` / `SuspendNearCacheOperations` for provider-neutral two-tier cache contracts.
- Suspend resilience decorators do not retry `CancellationException`; coroutine cancellation is propagated immediately.

## `testFixtures` Usage Guide

`cache-core` is also suitable for shared test helpers and fixtures in modules that need consistent cache contracts during tests. Reuse the abstractions from this module rather than duplicating provider-neutral helpers in each backend-specific module.
