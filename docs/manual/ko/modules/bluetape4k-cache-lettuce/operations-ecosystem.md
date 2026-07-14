---
title: 수명주기, 테스트와 생태계 경로
description: Redis 장애, resource 종료, 검증 순서와 cache-core·Hibernate·Spring·Exposed·workshop 경로를 연결합니다.
manualId: bluetape4k-cache-lettuce
chapterId: operations-ecosystem
---

# 수명주기, 테스트와 생태계 경로

## 소유권을 표로 고정하기

| 생성 경로 | cache connection | RedisClient | 종료 주체 |
| --- | --- | --- | --- |
| `LettuceCachingProvider` | manager가 생성 | provider가 생성 | provider/manager close |
| `LettuceJCaching` | manager가 생성 | 애플리케이션이 전달 | cache/manager와 애플리케이션 |
| `LettuceNearCache` | cache instance가 생성 | 애플리케이션이 전달 | cache close 후 애플리케이션 |
| memoizer용 `LettuceMap` | caller가 connection으로 생성 | 애플리케이션이 전달 | caller |

Near Cache `close()`는 tracking을 끄고 connection과 Caffeine L1을 닫습니다. RedisClient는 닫지 않습니다. JCache `close()`는 Redis hash를 삭제하지 않지만 manager `destroyCache()`는 데이터를 비웁니다.

## Redis 장애를 해석하기

기본 `LettuceNearCache`의 L1 hit는 Redis를 호출하지 않습니다. L1 miss, write와 remove는 Redis 오류를 그대로 전파합니다. tracking 시작 실패만 fail-open으로 처리합니다.

공통 `withResilience` extension은 `cache-core`의 `ResilientNearCacheDecorator`를 씌웁니다.

```kotlin
val resilient = LettuceCaches.nearCache<User>(redisClient) {
    cacheName = "users"
}.withResilience(
    NearCacheResilienceConfig(
        getFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL,
    )
)
```

이 decorator의 retry와 fallback 계약을 먼저 읽습니다. Redis GET 실패 때 `null`을 반환하면 caller는 cache miss로 보고 database를 조회할 수 있습니다. Redis가 잠깐 불안정되었을 때 모든 instance가 동시에 원본 저장소로 몰리면 캐시가 장애 증폭기가 됩니다. retry 수, timeout, database pool과 동시 load 제한을 함께 설계합니다.

## codec과 데이터 수명

기본 binary codec은 LZ4+Fory입니다. Redis에 저장된 byte는 애플리케이션 배포보다 오래 남을 수 있으므로 class 변경, serializer 등록과 신뢰 경계를 검토합니다.

- 호환되지 않는 변경에는 새 cache 이름이나 prefix를 사용합니다.
- Redis를 신뢰하지 못하는 경계에서는 임의 객체 역직렬화를 피합니다.
- JCache hash와 Near Cache per-key 데이터는 저장 모양이 다르므로 같은 이름을 공유하지 않습니다.
- TTL 없는 cache에는 명시적인 eviction과 배포 migration 절차를 둡니다.

## 운영 지표

- L1 hit·miss·eviction, local size와 max size
- Redis hit·miss, command latency, timeout, reconnect와 connection 수
- CLIENT TRACKING start failure, invalidation 도착 시간과 stale read
- `SCAN`/`UNLINK`를 쓰는 `clearAll`, `backCacheSize` 실행 시간
- memoizer evaluator latency·failure·cancel과 hot key 집중도
- fallback 뒤 database query latency, pool saturation과 request error

cache hit ratio만 높아도 오래된 값이 반환될 수 있습니다. 데이터 변경 뒤 다른 instance의 L1이 실제로 지워지는지를 별도 synthetic check로 확인합니다.

## 테스트 순서

1. `LettuceJCachesTest`로 필요한 factory가 예상 타입을 만드는지 확인합니다.
2. JCache를 쓴다면 manager identity, TTL, typed lookup, close/destroy를 검증합니다.
3. memoizer를 쓴다면 same-key 경쟁, evaluator 실패와 cancellation을 검증합니다.
4. Near Cache를 쓴다면 L1/L2 CRUD와 cacheName 격리를 확인합니다.
5. RESP3 환경에서 두 cache instance와 외부 writer의 invalidation을 실행합니다.
6. Redis 중단·복구 시 fallback과 원본 저장소 부하를 애플리케이션 수준에서 측정합니다.

```bash
./gradlew :bluetape4k-cache-lettuce:test --no-build-cache --no-configuration-cache
```

이 task는 Redis Testcontainers를 사용하므로 다른 Testcontainers/real DB suite와 순차 실행합니다.

## 생태계 경로

### 기반 계약과 Redis API

- [`bluetape4k-cache-core`](../bluetape4k-cache-core.md): JCache, memoizer, Near Cache interface와 resilience decorator
- [`bluetape4k-lettuce`](../bluetape4k-lettuce.md): RedisClient, connection, codec, map, coroutine command와 Lua script

### ORM과 Spring Boot

- [`bluetape4k-hibernate-cache-lettuce`](../bluetape4k-hibernate-cache-lettuce.md): Hibernate entity·collection·query Region을 L1/L2에 연결
- [`bluetape4k-spring-boot-hibernate-lettuce`](../bluetape4k-spring-boot-hibernate-lettuce.md): properties, auto-configuration, metrics와 actuator
- [`Hibernate Lettuce demo`](../bluetape4k-spring-boot-hibernate-lettuce-demo.md): 실제 Spring Data entity와 endpoint

Hibernate 2차 캐시는 SessionFactory가 Region 수명주기를 관리합니다. 직접 `LettuceNearCache`를 만드는 애플리케이션 cache와 같은 방식으로 종료하거나 key를 수정하지 않습니다.

### Exposed와 workshop

[bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)와 [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)은 repository boundary에서 cache-aside, loader/writer와 database transaction을 함께 다루는 다음 단계입니다. [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)은 서비스 예제로 확장합니다.

일반 `put`은 cache 계층만 갱신합니다. database까지 포함한 read-through·write-through·write-behind를 설명할 때는 `JdbcCacheRepository`, `EntityMapLoader`, `EntityMapWriter` 같은 실제 repository 경계를 확인합니다.

## Source와 tests

- [`LettuceCaches.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/LettuceCaches.kt)
- [`LettuceNearCacheFactory.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheFactory.kt)
- [`ResilientLettuceNearCacheOpsTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientLettuceNearCacheOpsTest.kt)
- [`LettuceNearCacheIsolationTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheIsolationTest.kt)
- [`LettuceNearCacheTrackingTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheTrackingTest.kt)
