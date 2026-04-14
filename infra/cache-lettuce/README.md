# Module bluetape4k-cache-lettuce

English | [한국어](./README.ko.md)

`bluetape4k-cache-lettuce` provides a Lettuce (Redis)-based JCache provider and NearCache implementations.

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-lettuce:${bluetape4kVersion}")
}
```

## Provided Features

- sync / async / suspend memoizers built on `LettuceMap`
- Lettuce-based `CachingProvider` and `LettuceJCaching`
- blocking and coroutine two-tier near caches
- resilient near-cache variants with write-behind and retry
- JCache-based `NearJCache` / `SuspendNearJCache`
- RESP3 `CLIENT TRACKING` invalidation support

## Factory (`LettuceCaches`)

`LettuceCaches` exposes factory methods for:

- `jcache`
- `suspendCache`
- `nearJCache`
- `suspendNearJCache`
- `nearCache`
- `suspendNearCache`
- resilient near-cache variants

## Usage Examples

Typical examples include:

- memoizer creation for Redis-backed function caching
- `NearJCacheConfig` DSL usage
- sync / suspend JCache-backed near caches
- native `LettuceNearCache` / `LettuceSuspendNearCache`

## Architecture Diagrams

### LettuceNearCache Class Hierarchy

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

    class LettuceNearCache {
        -config: LettuceNearCacheConfig
        -frontCache: LettuceCaffeineLocalCache
        -trackingListener: TrackingInvalidationListener
    }

    class LettuceSuspendNearCache {
        -config: LettuceNearCacheConfig
        -frontCache: LettuceCaffeineLocalCache
        -trackingListener: TrackingInvalidationListener
    }

    class LettuceLocalCache {
        <<interface>>
        +get(key: K) V?
        +put(key: K, value: V)
        +remove(key: K)
        +clear()
    }

    class LettuceCaffeineLocalCache {
        -cache: Cache
        +invalidate(key: String)
    }

    class TrackingInvalidationListener {
        -frontCache: LettuceLocalCache
        -connection: StatefulRedisConnection
        +start()
        +close()
    }

    NearCacheOperations <|.. LettuceNearCache
    SuspendNearCacheOperations <|.. LettuceSuspendNearCache
    LettuceLocalCache <|.. LettuceCaffeineLocalCache
    LettuceNearCache --> LettuceCaffeineLocalCache: frontCache
    LettuceNearCache --> TrackingInvalidationListener: trackingListener
    LettuceSuspendNearCache --> LettuceCaffeineLocalCache: frontCache
    LettuceSuspendNearCache --> TrackingInvalidationListener: trackingListener
    TrackingInvalidationListener --> LettuceCaffeineLocalCache: invalidates

    style NearCacheOperations fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style SuspendNearCacheOperations fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style LettuceNearCache fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style LettuceSuspendNearCache fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style LettuceLocalCache fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style LettuceCaffeineLocalCache fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style TrackingInvalidationListener fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
```

### RESP3 CLIENT TRACKING Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant NC as LettuceNearCache
    participant L1 as Caffeine (L1)
    participant TL as TrackingInvalidationListener
    participant R as Redis Server

    App->>NC: get("key")
    NC->>L1: get("key")
    alt L1 hit
        L1-->>NC: value
        NC-->>App: value
    else L1 miss
        L1-->>NC: null
        NC->>R: GET key
        alt hit
            R-->>NC: value
            NC->>L1: put("key", value)
            NC-->>App: value
        else miss
            R-->>NC: null
            NC-->>App: null
        end
    end

    Note over R,TL: Server-pushed invalidation (CLIENT TRACKING)
    R-)TL: INVALIDATE ["key"]
    TL->>L1: invalidate("key")
```

## `ResilientLettuceNearCacheConfig` Options

The resilient configuration extends the standard near-cache config with write-behind queueing, retry settings, and graceful-degradation behavior.

## `LettuceNearCacheConfig` Options

Core options include:

- `cacheName`
- `maxLocalSize`
- `frontExpireAfterWrite`
- `redisTtl`
- `useRespProtocol3`
- `recordStats`

Validation rules:

- batch size, queue size, retry count, and local cache size must be greater than zero
- TTL must be greater than zero when configured
- cache names and key prefixes must not be blank

## Key Isolation Strategy

Redis keys are namespaced through the configured cache name and key prefix so multiple caches can coexist safely in the same Redis deployment.

## Notes

- Use `NearJCache` / `SuspendNearJCache` when JCache standard compatibility is important.
- Use `LettuceNearCache` / `LettuceSuspendNearCache` when richer stats and resilience features matter.
- RESP3 `CLIENT TRACKING` is the basis for automatic invalidation.
