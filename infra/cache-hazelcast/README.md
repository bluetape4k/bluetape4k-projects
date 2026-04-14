# Module bluetape4k-cache-hazelcast

English | [한국어](./README.ko.md)

`bluetape4k-cache-hazelcast` provides a Hazelcast-based JCache provider, coroutine-friendly cache implementations, and a
**Caffeine + Hazelcast IMap 2-tier near cache**.

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

`NearJCache<K,V>` and
`SuspendNearJCache<K,V>` directly implement the JCache interface with a Caffeine(front) + Hazelcast IMap(back) structure.

The Korean README includes the full class diagrams and listener-related notes, including why `SuspendNearJCache` uses
`withoutListener(front, back)` for the Hazelcast client case.

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

## Architecture Diagrams

### HazelcastNearCache Class Hierarchy

```mermaid
classDiagram
    class NearCacheOperations {
        <<interface>>
        +get(key: String) V?
        +put(key: String, value: V)
        +remove(key: String)
        +clearLocal()
        +clearAll()
        +stats() NearCacheStatistics
    }

    class SuspendNearCacheOperations {
        <<interface>>
        +get(key: String) V?
        +put(key: String, value: V)
        +remove(key: String)
        +clearLocal()
        +clearAll()
        +stats() NearCacheStatistics
    }

    class HazelcastNearCache {
        -imap: IMap
        -frontCache: CaffeineHazelcastLocalCache
        -entryListener: HazelcastEntryEventListener
        -config: HazelcastNearCacheConfig
    }

    class HazelcastSuspendNearCache {
        -imap: IMap
        -frontCache: CaffeineHazelcastLocalCache
        -entryListener: HazelcastEntryEventListener
        -config: HazelcastNearCacheConfig
    }

    class HazelcastLocalCache {
        <<interface>>
        +get(key: K) V?
        +put(key: K, value: V)
        +remove(key: K)
        +clear()
    }

    class CaffeineHazelcastLocalCache {
        -cache: Cache
        +invalidate(key: String)
    }

    class HazelcastEntryEventListener {
        -frontCache: HazelcastLocalCache
        +entryUpdated(event)
        +entryRemoved(event)
    }

    NearCacheOperations <|.. HazelcastNearCache
    SuspendNearCacheOperations <|.. HazelcastSuspendNearCache
    HazelcastLocalCache <|.. CaffeineHazelcastLocalCache
    HazelcastNearCache --> CaffeineHazelcastLocalCache: frontCache
    HazelcastNearCache --> HazelcastEntryEventListener: entryListener
    HazelcastSuspendNearCache --> CaffeineHazelcastLocalCache: frontCache
    HazelcastSuspendNearCache --> HazelcastEntryEventListener: entryListener
    HazelcastEntryEventListener --> HazelcastLocalCache: invalidates

    style NearCacheOperations fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style SuspendNearCacheOperations fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style HazelcastNearCache fill:#00897B,stroke:#00695C,color:#FFFFFF
    style HazelcastSuspendNearCache fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    style HazelcastLocalCache fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style CaffeineHazelcastLocalCache fill:#2E7D32,stroke:#1B5E20,color:#FFFFFF
    style HazelcastEntryEventListener fill:#37474F,stroke:#263238,color:#FFFFFF
```

### 2-Tier NearCache Flow

```mermaid
sequenceDiagram
    box rgb(232,245,233) Application
    participant App as Application
    end
    box rgb(227,242,253) NearCache
    participant NC as HazelcastNearCache
    participant L1 as Caffeine (L1)
    end
    box rgb(243,229,245) Hazelcast Cluster
    participant L2 as IMap (L2)
    end

    App->>NC: get("key")
    NC->>L1: get("key")
    alt L1 hit
        L1-->>NC: value
        NC-->>App: value
    else L1 miss
        L1-->>NC: null
        NC->>L2: get("key")
        alt L2 hit
            L2-->>NC: value
            NC->>L1: put("key", value)
            NC-->>App: value
        else L2 miss
            L2-->>NC: null
            NC-->>App: null
        end
    end

    Note over L2,L1: Invalidation via EntryListener
    L2-)NC: EntryEvent (updated/removed)
    NC->>L1: invalidate("key")
```

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
