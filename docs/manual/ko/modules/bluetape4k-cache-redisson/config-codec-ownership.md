---
title: 설정, codec과 client 소유권
description: Near Cache 기본값, 유효성 검증, wire format과 Redisson client lifecycle을 정합니다.
manualId: bluetape4k-cache-redisson
chapterId: config-codec-ownership
---

# 설정, codec과 client 소유권

## 기본값부터 명시한다

`RedissonNearCacheConfig`의 1.12.1 기본값은 다음과 같습니다.

| 설정 | 기본값 | 의미 |
| --- | --- | --- |
| `cacheName` | `redisson-near-cache` | Redis map 이름 |
| `maxLocalSize` | `10_000` | JVM local 최대 entry 수 |
| `timeToLive` | `null` | Redis TTL 없음 |
| `maxIdle` | `null` | idle expiry 없음 |
| `syncStrategy` | `INVALIDATE` | 다른 client 변경 시 local entry 무효화 |
| `reconnectionStrategy` | `CLEAR` | reconnect 시 local entry 제거 |
| `evictionPolicy` | `LRU` | local eviction |

운영에서는 기본 cache name을 그대로 공유하지 말고 서비스·데이터·환경을 식별하는 이름을 줍니다. TTL과 idle이 모두 `null`이면 Redis entry가 시간으로 만료되지 않습니다.

## 유효성 검증

cache name은 blank일 수 없고 `maxLocalSize`는 양수여야 합니다. TTL과 max idle을 지정한다면 `Duration.ZERO`보다 커야 합니다. data class 생성자와 DSL builder 모두 이 계약을 적용합니다.

```kotlin
val config = redissonNearCacheConfig {
    cacheName = "catalog:products:v1"
    maxLocalSize = 2_000
    timeToLive = Duration.ofMinutes(15)
    maxIdle = Duration.ofMinutes(5)
}
```

TTL은 Redis entry, `maxLocalSize`와 eviction은 각 JVM local tier에 영향을 줍니다. 둘을 같은 capacity 설정으로 생각하지 않습니다.

## codec은 데이터 계약이다

Near Cache 기본 codec은 `RedissonCodecs.LZ4Fory`입니다. codec은 압축 옵션이면서 wire format입니다. 배포 중 서로 다른 codec이 같은 map을 읽으면 decode failure가 발생할 수 있으므로 map 이름 versioning 또는 호환 가능한 rollout이 필요합니다.

JCache는 `Configuration<K, V>`에 key/value type과 loader/writer, expiry를 담습니다. 이미 같은 이름 cache가 존재하면 새 configuration이 덮어써진다고 가정하지 않습니다.

## 누가 client를 닫는가

일반적으로 애플리케이션의 DI container가 `RedissonClient`를 하나 만들고 종료합니다. `RedissonNearCache.close()`나 memoizer `clear()`는 client shutdown이 아닙니다. wrapper를 여러 곳에서 만들더라도 client owner는 하나로 둡니다.

`Config`로 JCache를 만드는 overload는 provider가 내부 client 생성에 관여할 수 있으므로 global `CacheManager`와 provider close 시점을 별도로 검증합니다. 테스트마다 고유 cache 이름을 사용해 singleton manager 상태가 섞이지 않게 합니다.

## Source와 tests

- [`RedissonNearCacheConfig.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheConfig.kt)
- [`RedissonCaches.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/RedissonCaches.kt)
- [`RedissonNearCacheConfigTest.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheConfigTest.kt)
