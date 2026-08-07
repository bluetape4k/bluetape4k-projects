---
title: Cache mode와 DB persistence
description: Cache-aside와 MapLoader/MapWriter 기반 read-through, write-through, write-behind의 실제 경계를 구분합니다.
manualId: bluetape4k-redisson
chapterId: cache-modes-persistence
---

# Cache mode와 DB persistence

## 이름보다 data flow를 본다

cache 전략은 설정 상수 이름이 아니라 miss와 write가 어디로 흐르는지로 구분합니다.

| 전략 | Read miss | Write | 일관성 책임 |
| --- | --- | --- | --- |
| Cache-aside | application이 DB를 읽고 cache에 넣음 | application이 DB와 cache를 순서대로 갱신 | application service |
| Read-through | `MapLoader`가 DB를 읽음 | 별도 정책 | loader와 cache configuration |
| Write-through | cache write가 `MapWriter`의 DB write 완료를 기다림 | 동기 DB 반영 | writer failure가 caller에 전파됨 |
| Write-behind | cache write 후 queue에서 DB 반영 | 비동기·batch DB 반영 | backlog, retry, 종료 drain |

`RMap.put()` 뒤 repository를 호출하는 코드는 cache-aside입니다. 이를 write-through라고 부르면 failure와 transaction 경계를 잘못 설명하게 됩니다.

## RedissonCacheConfig가 하는 일

`RedissonCacheConfig`는 cache mode, Redisson `WriteMode`, retry, local-cache option을 모읍니다. `toMapOptions`와 `toLocalCachedMapOptions`는 write mode를 options에 옮기지만 loader와 writer 구현을 만들어 주지 않습니다.

```kotlin
val config = RedissonCacheConfig.WRITE_BEHIND.copy(
    writeBehindBatchSize = 100,
    writeBehindDelay = 500,
)

val options = config.toMapOptions<String, User>("users")
    .loader(userLoader)
    .writer(userWriter)

val users = client.getMap(options)
```

실제 API에 연결할 `MapLoader`와 `MapWriter`는 DB repository가 제공합니다. loader는 miss에서 entity를 읽고, writer는 write/delete를 DB transaction 경계에 맞춰 수행해야 합니다.

## 지원하지 않는 설정은 fail-fast 한다

1.12.1 option 변환은 `ttl`, `maxSize`, `deleteFromDBOnInvalidate`를 직접 적용할 수 없습니다. 기본값이 아닌 값이 들어오면 `IllegalArgumentException`을 던집니다. 조용히 무시하지 않으므로 잘못된 일관성 기대를 배포 전에 찾을 수 있습니다.

- entry TTL은 `RMapCache`의 entry expiration API를 사용합니다.
- local size와 TTL은 `nearCacheMaxSize`, `nearCacheTtl`, `nearCacheMaxIdleTime`으로 설정합니다.
- DB delete-on-invalidate는 application/repository policy로 구현합니다.

## Write-behind의 운영 계약

write-behind는 request latency를 낮출 수 있지만 DB가 즉시 최신이라는 보장을 포기합니다. process crash, Redis 장애, writer exception에서 아직 반영되지 않은 write가 남거나 유실될 수 있습니다.

다음 항목을 설계하지 않았다면 write-through 또는 명시적 cache-aside가 안전합니다.

- 같은 key의 write 순서와 coalescing
- retry 후에도 안전한 idempotent DB write
- backlog와 oldest pending age metric
- 종료 시 drain timeout
- cache에는 성공했지만 DB에는 실패한 record의 복구 경로

## Exposed와 workshop으로 발전하기

반복되는 entity cache에는 repository abstraction이 필요합니다. [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)는 공통 계약인 `JdbcCacheRepository`와 Redisson용 `AbstractJdbcRedissonRepository`, `ExposedEntityMapLoader`, `ExposedEntityMapWriter`를 제공합니다. [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)의 11장 cache strategy 예제는 이 경계를 `RMap`/`RLocalCachedMap`과 Exposed table에 연결합니다. 이 구현을 기준으로 read/write-through를 학습하고 단순 `put` 예제를 canonical persistence pattern으로 사용하지 않습니다.

## Source와 tests

- [`RedissonCacheConfig.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/RedissonCacheConfig.kt)
- [`MapCacheSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/MapCacheSupport.kt)
- [`RedissonCacheConfigTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/cache/RedissonCacheConfigTest.kt)
- [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)
- [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)
