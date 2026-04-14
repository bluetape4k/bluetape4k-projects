# Module bluetape4k-cache

English | [한국어](./README.ko.md)

`bluetape4k-cache` is an umbrella module that bundles the cache-related modules together.

> The cache modules were consolidated from 10 modules into 5 modules plus this umbrella. Each distributed cache module now includes near-cache functionality.

## Module Composition

| Module                       | Provided Functionality                                                               |
|------------------------------|--------------------------------------------------------------------------------------|
| `bluetape4k-cache-core`      | JCache abstraction + Caffeine/Cache2k/Ehcache local caches + memorizer               |
| `bluetape4k-cache-hazelcast` | Hazelcast distributed cache + near cache (merged from former `cache-hazelcast-near`) |
| `bluetape4k-cache-redisson`  | Redisson distributed cache + near cache (merged from former `cache-redisson-near`)   |
| `bluetape4k-cache-lettuce`   | Lettuce (Redis) distributed cache + near cache                                       |

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache:${bluetape4kVersion}")
}
```

Because this module pulls in every provider, it is usually better to depend directly on a provider module when you only need one.

## Recommended Selective Dependencies

### 1. If You Only Need a Local Cache

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-core:${bluetape4kVersion}")
}
```

### 2. Redisson Distributed Cache + Near Cache

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-redisson:${bluetape4kVersion}")
}
```

### 3. Hazelcast Distributed Cache + Near Cache

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-hazelcast:${bluetape4kVersion}")
}
```

## Quick Start

### 1. Caffeine Local Cache

```kotlin
import io.bluetape4k.cache.jcache.JCaching

val cache = JCaching.Caffeine.getOrCreate<String, Any>("users")
cache.put("u:1", mapOf("name" to "debop"))
```

### 2. Hazelcast Near Cache (Coroutine)

```kotlin
import io.bluetape4k.cache.nearcache.hazelcast.coroutines.HazelcastNearSuspendCache

val near = HazelcastNearSuspendCache<String, Any>("hz-users-near", hazelcastInstance)
near.put("key", "value")
val value = near.get("key")
```

### 3. Redisson Near Cache (Coroutine)

```kotlin
import io.bluetape4k.cache.nearcache.redis.coroutines.RedissonNearSuspendCache

val near = RedissonNearSuspendCache<String, Any>("redis-users-near", redissonClient)
near.put("key", "value")
val value = near.get("key")
```

## Module Dependency Structure

```mermaid
flowchart TD
    A[bluetape4k-cache<br/>umbrella] --> B[bluetape4k-cache-core<br/>JCache abstraction + local cache]
    A --> C[bluetape4k-cache-hazelcast<br/>Hazelcast distributed cache + NearCache]
    A --> D[bluetape4k-cache-redisson<br/>Redisson distributed cache + NearCache]
    A --> E[bluetape4k-cache-lettuce<br/>Lettuce Redis distributed cache + NearCache]

    B --> B1[Caffeine local cache]
    B --> B2[Cache2k local cache]
    B --> B3[Ehcache local cache]
    B --> B4[NearCacheOperations interface]
    B --> B5[SuspendNearCacheOperations interface]

    C --> C1[HazelcastNearCache]
    C --> C2[HazelcastSuspendNearCache]

    D --> D1[RedissonNearCache<br/>based on RLocalCachedMap]
    D --> D2[RedissonSuspendNearCache]
    D --> D3[RedissonResp3NearCache<br/>RESP3 hybrid]

    E --> E1[LettuceNearCache<br/>RESP3 CLIENT TRACKING]
    E --> E2[LettuceSuspendNearCache]

    classDef umbrellaStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef implStyle fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    classDef suspendStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef interfaceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0

    style A fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style B fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style C fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style D fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style E fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style B1 fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style B2 fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style B3 fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style B4 fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style B5 fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style C1 fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style C2 fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style D1 fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style D2 fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style D3 fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    style E1 fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style E2 fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
```

## Unified NearCache Interface Hierarchy

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

    class HazelcastNearCache {
        Caffeine + IMap (EntryListener invalidation)
    }

    class HazelcastSuspendNearCache {
        Caffeine + IMap (coroutine)
    }

    class RedissonNearCache {
        RLocalCachedMap (built-in pub/sub invalidation)
    }

    class RedissonSuspendNearCache {
        RLocalCachedMap (coroutine)
    }

    class RedissonResp3NearCache {
        Redisson RBucket + Lettuce RESP3 tracking
    }

    class LettuceNearCache {
        Caffeine + Redis (RESP3 CLIENT TRACKING)
    }

    class LettuceSuspendNearCache {
        Caffeine + Redis RESP3 (coroutine)
    }

    NearCacheOperations <|.. HazelcastNearCache
    NearCacheOperations <|.. RedissonNearCache
    NearCacheOperations <|.. RedissonResp3NearCache
    NearCacheOperations <|.. LettuceNearCache
    SuspendNearCacheOperations <|.. HazelcastSuspendNearCache
    SuspendNearCacheOperations <|.. RedissonSuspendNearCache
    SuspendNearCacheOperations <|.. LettuceSuspendNearCache

    style NearCacheOperations fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style SuspendNearCacheOperations fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style HazelcastNearCache fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style HazelcastSuspendNearCache fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style RedissonNearCache fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style RedissonSuspendNearCache fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style RedissonResp3NearCache fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    style LettuceNearCache fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style LettuceSuspendNearCache fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A

```

## Near Cache 2-Tier Architecture

```mermaid
flowchart LR
    App[Application] -->|get| LocalCache[Local cache<br/>Caffeine/Cache2k]
    LocalCache -->|cache hit| App
    LocalCache -->|cache miss| RemoteCache[Remote cache<br/>Redis / Hazelcast]
    RemoteCache -->|return data| LocalCache
    App -->|put| RemoteCache
    RemoteCache -->|propagate invalidation| LocalCache

    classDef appStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef localStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef remoteStyle fill:#E0F2F1,stroke:#80CBC4,color:#00695C

    style App fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style LocalCache fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style RemoteCache fill:#E0F2F1,stroke:#80CBC4,color:#00695C
```

## Caution About Automatic `CachingProvider` Loading

Multiple modules register
`META-INF/services/javax.cache.spi.CachingProvider`. When using the umbrella module, specify the provider explicitly:

```kotlin
import javax.cache.Caching

val provider = Caching.getCachingProvider("io.bluetape4k.cache.nearcache.redis.RedissonNearCachingProvider")
val manager = provider.cacheManager
```

In Spring Boot, configure it through `application.properties`:

```properties
spring.cache.jcache.provider=io.bluetape4k.cache.nearcache.redis.RedissonNearCachingProvider
```
