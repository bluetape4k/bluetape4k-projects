# Module bluetape4k-cache-lettuce

`bluetape4k-cache-lettuce`는 Lettuce(Redis) 기반 JCache Provider와 NearCache 구현을 제공합니다.

## 제공 기능

### JCache (JSR-107)

- Lettuce 기반 `javax.cache.spi.CachingProvider` 구현
- `LettuceJCaching` — Lettuce JCache 설정 및 CachingProvider 초기화 헬퍼

### NearCache (2-Tier Cache)

Caffeine(로컬) + Redis(분산) 2단계 캐시로, RESP3 CLIENT TRACKING을 통한 자동 invalidation을 지원합니다.

| 클래스                               | 설명                               |
|-----------------------------------|----------------------------------|
| `LettuceNearCache<V>`             | 동기(Blocking) 2-Tier 캐시           |
| `LettuceNearSuspendCache<V>`      | Coroutines(suspend) 2-Tier 캐시    |
| `NearCacheConfig<K, V>`           | NearCache 설정 data class + DSL 빌더 |
| `LocalCache<K, V>`                | front cache 추상 인터페이스             |
| `CaffeineLocalCache<K, V>`        | Caffeine 기반 LocalCache 구현        |
| `TrackingInvalidationListener<V>` | RESP3 CLIENT TRACKING push 리스너   |

### NearCache 아키텍처

```
Application
    |
[LettuceNearCache / LettuceNearSuspendCache]
    |
+---+---+
|       |
Front   Back
Caffeine  Redis (via Lettuce RESP3)

Invalidation: Redis CLIENT TRACKING → server push → local invalidate
```

- **Read**: front hit → 즉시 반환 / front miss → Redis GET → front populate → 반환
- **Write**: front put + Redis SET (write-through)
- **Invalidation**: RESP3 CLIENT TRACKING push → 로컬 캐시 자동 무효화

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-cache-lettuce:${bluetape4kVersion}")
}
```

## 사용 예시

### NearCacheConfig DSL

```kotlin
import io.bluetape4k.cache.nearcache.lettuce.nearCacheConfig

val config = nearCacheConfig<String, String> {
    cacheName = "my-cache"
    maxLocalSize = 10_000
    frontExpireAfterWrite = Duration.ofMinutes(30)
    redisTtl = Duration.ofHours(1)
    useRespProtocol3 = true  // CLIENT TRACKING 활성화
}
```

### 동기 NearCache

```kotlin
import io.bluetape4k.cache.nearcache.lettuce.LettuceNearCache
import io.bluetape4k.cache.nearcache.lettuce.NearCacheConfig
import io.lettuce.core.RedisClient
import io.lettuce.core.ClientOptions
import io.lettuce.core.protocol.ProtocolVersion

// RESP3 활성화 RedisClient 생성
val redisClient = RedisClient.create("redis://localhost:6379").also {
    it.options = ClientOptions.builder()
        .protocolVersion(ProtocolVersion.RESP3)
        .build()
}

val cache = LettuceNearCache(
    redisClient = redisClient,
    config = NearCacheConfig(cacheName = "orders"),
)

cache.use { c ->
    c.put("order-1", "data")
    val value = c.get("order-1")  // front hit (로컬)
    c.remove("order-1")
}
```

### Coroutines NearCache

```kotlin
import io.bluetape4k.cache.nearcache.lettuce.LettuceNearSuspendCache
import io.bluetape4k.cache.nearcache.lettuce.NearCacheConfig

val cache = LettuceNearSuspendCache(
    redisClient = redisClient,
    config = NearCacheConfig(cacheName = "sessions"),
)

cache.use { c ->
    c.put("session-1", "token-abc")
    val token = c.get("session-1")  // suspend fun
    c.clearAll()
}
```

## NearCacheConfig 옵션

| 옵션                       | 기본값                    | 설명                               |
|--------------------------|------------------------|----------------------------------|
| `cacheName`              | `"lettuce-near-cache"` | 캐시 이름 (Redis key prefix, `:` 금지) |
| `maxLocalSize`           | `10_000`               | Caffeine 최대 항목 수                 |
| `frontExpireAfterWrite`  | `30분`                  | 로컬 캐시 write 후 만료 시간              |
| `frontExpireAfterAccess` | `null`                 | 로컬 캐시 access 후 만료 시간             |
| `redisTtl`               | `null`                 | Redis TTL (null이면 영구 보존)         |
| `useRespProtocol3`       | `true`                 | RESP3 CLIENT TRACKING 활성화 여부     |
| `recordStats`            | `false`                | Caffeine 통계 수집 여부                |

## Key 격리 전략

Redis key는 `{cacheName}:{key}` 형태의 prefix로 저장됩니다.

```
cacheName="orders", key="user:123" → Redis key: "orders:user:123"
```

- `clearAll()`은 SCAN으로 해당 cacheName의 key만 삭제 (FLUSHDB 사용 안 함)
- 서로 다른 cacheName 인스턴스는 완전히 독립적인 key 공간을 가짐
- key에 `:` 포함 가능 (cacheName에만 `:` 금지)

## 참고

- RESP3 CLIENT TRACKING은 Redis 6.0+ 이상에서 지원됩니다.
- NearCache는 단일 Redis 연결에서 동작하며, 클러스터 모드에서는 별도 설정이 필요합니다.
- 다른 분산 캐시 백엔드가 필요한 경우:
  - Redisson 기반: `bluetape4k-cache-redisson`
  - Hazelcast 기반: `bluetape4k-cache-hazelcast`
  - Apache Ignite 기반: `bluetape4k-cache-ignite`
