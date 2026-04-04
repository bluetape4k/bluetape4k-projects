# Module bluetape4k-cache-hazelcast

English | [한국어](./README.ko.md)

`bluetape4k-cache-hazelcast` provides a Hazelcast-based JCache provider, coroutine-friendly cache implementations, and a **Caffeine + Hazelcast IMap 2-tier near cache**.

> The former `bluetape4k-cache-hazelcast-near` module was merged into this module.

## Provided Features

- `HazelcastJCaching`
- `HazelcastSuspendCache`
- `HazelcastNearCache<V>`
- `HazelcastSuspendNearCache<V>`
- `ResilientHazelcastNearCache<V>`
- `ResilientHazelcastSuspendNearCache<V>`
- `HazelcastNearCacheConfig`
- `ResilientHazelcastNearCacheConfig`
- `HazelcastLocalCache<V>`
- `CaffeineHazelcastLocalCache<V>`
- `HazelcastEntryEventListener`
- sync / async / suspend memoizers based on `IMap`

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-hazelcast:${bluetape4kVersion}")
}
```

## Factory (`HazelcastCaches`)

`HazelcastCaches` offers convenient factory functions for JCache, suspend cache, near cache, and resilient near-cache variants.

## JCache-Based NearCache (`nearcache.jcache` package)

`NearJCache<K,V>` and `SuspendNearJCache<K,V>` directly implement the JCache interface with a Caffeine(front) + Hazelcast IMap(back) structure.

The Korean README includes the full class diagrams and listener-related notes, including why `SuspendNearJCache` uses `withoutListener(front, back)` for the Hazelcast client case.

## Class Structure

The main pieces are:

- blocking and coroutine NearCache operation interfaces
- `HazelcastNearCache` / `HazelcastSuspendNearCache`
- `HazelcastLocalCache` and its Caffeine-backed implementation
- `HazelcastEntryEventListener` for invalidation
- `HazelcastNearCacheConfig` for sizing and expiration rules

## NearCache Architecture

Two main modes are supported:

- **Write-through**
  - front-cache hit returns immediately
  - front-cache miss reads from IMap and repopulates the front cache
  - writes update both front cache and IMap synchronously
  - invalidation is handled through `IMap` `EntryListener`

- **Write-behind (Resilient)**
  - front cache is updated immediately
  - remote writes are processed asynchronously through a queue
  - stale-read prevention and retry handling are built in

## Usage Examples

Typical usage includes:

- `HazelcastSuspendCache`
- `HazelcastNearCache`
- `HazelcastSuspendNearCache`
- resilient near-cache variants with retry configuration
- factory-based cache creation through `HazelcastCaches`

## `HazelcastNearCacheConfig` Options

Important options include:

- `cacheName`
- `maxLocalSize`
- `frontExpireAfterWrite`
- `frontExpireAfterAccess`
- `recordStats`

Validation rules from the Korean README still apply:

- `cacheName` must not be blank
- `maxLocalSize` must be greater than zero
- expiration durations must be greater than zero when configured

## `ResilientHazelcastNearCacheConfig` Options

Resilient configuration extends the normal near-cache configuration with queue sizes, retry behavior, failure-handling strategy, and write-behind controls.

## Registered `CachingProvider` List

When multiple JCache providers are present on the classpath, explicitly choose the Hazelcast provider when needed, especially in Spring or shared umbrella-module setups.
