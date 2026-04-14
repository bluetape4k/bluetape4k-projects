# Module bluetape4k-cache-core

English | [한국어](./README.ko.md)

`bluetape4k-cache-core` provides the shared cache API, core abstractions, and **local cache implementations**.

> The former `bluetape4k-cache-local` module was merged into this module.

## Provided Features

- **Common JCache utilities**: `JCaching`, `jcacheManager`, `jcacheConfiguration`, and more
- **Coroutines cache abstractions**: `SuspendCache`, `SuspendCacheEntry`
- **Unified NearCache interfaces**: `NearCacheOperations<V>`, `SuspendNearCacheOperations<V>`, `NearCacheStatistics`
- **Resilient decorators**: `ResilientNearCacheDecorator`, `ResilientSuspendNearCacheDecorator`
- **JCache NearCache**: `JCacheNearCache<V>`
- **Legacy Near Cache**: `NearCache<K,V>`, `SuspendNearCache<K,V>`
- **Memorizer and Memoizer abstractions** for sync, async, and suspend flows
- **Local cache providers**: Caffeine, Cache2k, and Ehcache

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-core:${bluetape4kVersion}")
}
```

Add the appropriate provider module if you need distributed caching.

## Detailed Features

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

## Recommended Usage Patterns

- Use `cache-core` directly when local cache and common abstractions are enough.
- Use provider modules such as Hazelcast, Lettuce, or Redisson when remote storage or invalidation is required.
- Prefer the newer `Memoizer` / `AsyncMemoizer` / `SuspendMemoizer` abstractions for new code.
- Use the legacy near-cache APIs only for backward compatibility.

## Architecture Diagrams

### NearCache get() Sequence (front miss → back lookup → front fill)

```mermaid
sequenceDiagram
    box rgb(187,222,251) Application
    participant App as Application
    end
    box rgb(178,223,219) NearCache Layer
    participant NC as NearCache
    participant Front as Front Cache (Caffeine)
    end
    box rgb(207,216,220) Remote Storage
    participant Back as Back Cache (Redis/IMap/Redisson)
    end
    App ->> NC: get("key")
    NC ->> Front: get("key")
    alt front hit
        Front -->> NC: value
        NC -->> App: value (immediate return)
    else front miss
        Front -->> NC: null
        NC ->> Back: get("key")
        alt back hit
            Back -->> NC: value
            NC ->> Front: put("key", value)
            Front -->> NC: ok
            NC -->> App: value
        else back miss
            Back -->> NC: null
            NC -->> App: null
        end
    end
```

### NearCache put() Sequence (write-through)

```mermaid
sequenceDiagram
    box rgb(187,222,251) Application
    participant App as Application
    end
    box rgb(178,223,219) NearCache Layer
    participant NC as NearCache
    participant Front as Front Cache (Caffeine)
    end
    box rgb(207,216,220) Remote Storage
    participant Back as Back Cache (Redis/IMap/Redisson)
    end
    App ->> NC: put("key", value)
    NC ->> Back: set("key", value)
    Back -->> NC: ok
    NC ->> Front: put("key", value)
    Front -->> NC: ok
    NC -->> App: (complete)
```

### NearCache Interface Hierarchy

```mermaid
classDiagram
    class NearCacheOperations {
        <<interface>>
        +cacheName: String
        +isClosed: Boolean
        +get(key: String) V?
        +put(key: String, value: V)
        +remove(key: String)
        +clearLocal()
        +clearAll()
        +stats() NearCacheStatistics
        +close()
    }

    class SuspendNearCacheOperations {
        <<interface>>
        +cacheName: String
        +isClosed: Boolean
        +get(key: String) V?
        +put(key: String, value: V)
        +remove(key: String)
        +clearLocal()
        +clearAll()
        +stats() NearCacheStatistics
        +close()
    }

    class NearCacheStatistics {
        <<interface>>
        +localHits: Long
        +localMisses: Long
        +localSize: Long
        +hitRate: Double
    }

    class ResilientNearCacheDecorator {
        -delegate: NearCacheOperations~V~
        -retry: Retry
        -config: NearCacheResilienceConfig
    }

    class ResilientSuspendNearCacheDecorator {
        -delegate: SuspendNearCacheOperations~V~
        -retry: Retry
    }

    NearCacheOperations <|.. ResilientNearCacheDecorator
    NearCacheOperations --o ResilientNearCacheDecorator : delegate
    NearCacheOperations ..> NearCacheStatistics : stats()
    SuspendNearCacheOperations <|.. ResilientSuspendNearCacheDecorator
    SuspendNearCacheOperations --o ResilientSuspendNearCacheDecorator : delegate

    style NearCacheOperations fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style SuspendNearCacheOperations fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style NearCacheStatistics fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style ResilientNearCacheDecorator fill:#AD1457,stroke:#880E4F,color:#FFFFFF
    style ResilientSuspendNearCacheDecorator fill:#AD1457,stroke:#880E4F,color:#FFFFFF
```

## `testFixtures` Usage Guide

`cache-core` is also suitable for shared test helpers and fixtures in modules that need consistent cache contracts during tests. Reuse the abstractions from this module rather than duplicating provider-neutral helpers in each backend-specific module.
