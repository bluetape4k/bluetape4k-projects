# Module bluetape4k-cache-hazelcast

`bluetape4k-cache-hazelcast`는 Hazelcast 기반 JCache Provider, Coroutines 캐시 구현, 그리고 **Caffeine + Hazelcast IMap 2-Tier Near Cache**를 제공합니다.

> 기존 `bluetape4k-cache-hazelcast-near` 모듈이 이 모듈에 통합되었습니다.

## 제공 기능

- **Hazelcast JCache Provider** (`HazelcastJCaching`)
- **`HazelcastSuspendCache`**: JCache 기반 코루틴 캐시
- **`HazelcastNearCache`**: Caffeine(로컬) + Hazelcast IMap(분산) 2-Tier Near Cache (동기)
- **`HazelcastSuspendNearCache`**: Near Cache 코루틴 구현
- **`HazelcastNearCacheConfig`**: Near Cache 설정 data class + DSL 빌더
- **`HazelcastLocalCache`**: front cache 추상 인터페이스
- **`CaffeineHazelcastLocalCache`**: Caffeine 기반 LocalCache 구현
- **`HazelcastEntryEventListener`**: IMap EntryListener 기반 invalidation 리스너

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-hazelcast:${bluetape4kVersion}")
}
```

## NearCache 아키텍처

```
Application
    |
[HazelcastNearCache / HazelcastSuspendNearCache]
    |
+--------+--------+-----------+
|        |        |           |
Front   Back    Listener
Caffeine  IMap   EntryListener
(local) (remote)  (invalidation)
```

- **Read**: front hit → 즉시 반환 / front miss → IMap GET → front populate → 반환
- **Write**: front put + IMap PUT (write-through)
- **Invalidation**: IMap EntryListener → 로컬 캐시 자동 무효화

> JCache `registerCacheEntryListener`는 리스너 factory를 서버로 직렬화해 전송하므로
> non-serializable 리스너가 실패한다. `IMap.addEntryListener`는 클라이언트 JVM에서 리스너를
> 실행하므로 직렬화가 불필요하다.

## 사용 예시

### 1. HazelcastSuspendCache

```kotlin
import io.bluetape4k.cache.jcache.HazelcastSuspendCache

val suspendCache = HazelcastSuspendCache<String, Any>("hazelcast-cache")
suspendCache.put("key", "value")
val value = suspendCache.get("key")
```

### 2. HazelcastNearCacheConfig DSL

```kotlin
import io.bluetape4k.cache.nearcache.hazelcastNearCacheConfig

val config = hazelcastNearCacheConfig {
    cacheName = "my-near-cache"
    maxLocalSize = 10_000
    frontExpireAfterWrite = Duration.ofMinutes(30)
    frontExpireAfterAccess = null
    recordStats = false
}
```

### 3. HazelcastNearCache (동기)

```kotlin
import io.bluetape4k.cache.nearcache.HazelcastNearCache
import io.bluetape4k.cache.nearcache.HazelcastNearCacheConfig

val cache = HazelcastNearCache<String>(
    hazelcastInstance = hazelcastClient,
    config = HazelcastNearCacheConfig(cacheName = "orders"),
)

cache.use { c ->
    c.put("order-1", "data")
    val value = c.get("order-1")  // 로컬 Caffeine에서 우선 조회
    c.remove("order-1")
}
```

### 4. HazelcastSuspendNearCache (코루틴)

```kotlin
import io.bluetape4k.cache.nearcache.HazelcastSuspendNearCache
import io.bluetape4k.cache.nearcache.HazelcastNearCacheConfig

val cache = HazelcastSuspendNearCache<String>(
    hazelcastInstance = hazelcastClient,
    config = HazelcastNearCacheConfig(cacheName = "sessions"),
)

cache.use { c ->
    c.put("session-1", "token-abc")
    val token = c.get("session-1")  // suspend fun
    c.clearAll()
}
```

## HazelcastNearCacheConfig 옵션

| 옵션                       | 기본값                      | 설명                                |
|--------------------------|--------------------------|-----------------------------------|
| `cacheName`              | `"hazelcast-near-cache"` | 캐시(IMap) 이름                       |
| `maxLocalSize`           | `10_000`                 | Caffeine 최대 항목 수                   |
| `frontExpireAfterWrite`  | `30분`                    | 로컬 캐시 write 후 만료 시간               |
| `frontExpireAfterAccess` | `null`                   | 로컬 캐시 access 후 만료 시간 (null이면 비활성) |
| `recordStats`            | `false`                  | Caffeine 통계 수집 여부                  |

## CachingProvider 등록 목록

`META-INF/services/javax.cache.spi.CachingProvider`에 등록된 Provider:

```
com.hazelcast.cache.HazelcastCachingProvider
```

클래스패스에 여러 Provider가 공존할 때는 명시적으로 지정하세요:

```kotlin
val provider = Caching.getCachingProvider("com.hazelcast.cache.HazelcastCachingProvider")
```
