# Module bluetape4k-cache

English | [한국어](./README.ko.md)

`bluetape4k-cache` is an umbrella module that bundles the cache-related modules together.

> The cache modules were consolidated from 10 modules into 5 modules plus this umbrella. Each distributed cache module now includes near-cache functionality.

## Module Composition

| Module | Provided Functionality |
|---|---|
| `bluetape4k-cache-core` | JCache abstraction + Caffeine/Cache2k/Ehcache local caches + memorizer |
| `bluetape4k-cache-hazelcast` | Hazelcast distributed cache + near cache (merged from former `cache-hazelcast-near`) |
| `bluetape4k-cache-redisson` | Redisson distributed cache + near cache (merged from former `cache-redisson-near`) |
| `bluetape4k-cache-lettuce` | Lettuce (Redis) distributed cache + near cache |

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

    style A fill:#2196F3
    style B fill:#4CAF50
    style C fill:#FF9800
    style D fill:#9C27B0
    style E fill:#F44336
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

    style LocalCache fill:#4CAF50
    style RemoteCache fill:#F44336
    style App fill:#2196F3
```

## Caution About Automatic `CachingProvider` Loading

Multiple modules register `META-INF/services/javax.cache.spi.CachingProvider`. When using the umbrella module, specify the provider explicitly:

```kotlin
import javax.cache.Caching

val provider = Caching.getCachingProvider("io.bluetape4k.cache.nearcache.redis.RedissonNearCachingProvider")
val manager = provider.cacheManager
```

In Spring Boot, configure it through `application.properties`:

```properties
spring.cache.jcache.provider=io.bluetape4k.cache.nearcache.redis.RedissonNearCachingProvider
```
