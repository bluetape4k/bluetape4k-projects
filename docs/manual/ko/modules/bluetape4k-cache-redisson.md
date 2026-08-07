---
manualId: bluetape4k-cache-redisson
title: "Redisson JCache와 Near Cache"
description: "Redisson JCache, Redis-backed memoizer, RLocalCachedMap Near Cache를 정확한 동시성·무효화·수명주기 계약과 함께 사용하는 방법을 설명합니다."
kind: library
group: caching
learningOrder: 530
---

# Redisson JCache와 Near Cache

## 제공하는 기능 {#problem}

`bluetape4k-cache-redisson`은 Redisson을 `bluetape4k-cache-core`의 공통 cache API에 연결합니다. JCache와 coroutine용 `SuspendJCache`, sync·async·suspend memoizer, `RLocalCachedMap` 기반 Near Cache를 제공합니다.

이 모듈이 해결하는 문제는 Redis 연결 자체가 아닙니다. Redisson client를 애플리케이션이 소유한 상태에서 **표준 JCache, 같은 key 계산 병합, JVM local entry의 Pub/Sub 무효화, coroutine 취소 전파**를 일관된 API로 다루는 데 초점을 둡니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 애플리케이션이 이미 Redisson을 사용하거나 Redisson의 distributed object와 codec을 함께 사용합니다.
- 완료된 계산 결과는 Redis에 공유하되 같은 key의 진행 중인 계산은 한 JVM 안에서만 합치면 됩니다.
- 별도 local cache와 invalidation listener를 직접 조립하지 않고 `RLocalCachedMap`의 Pub/Sub 동기화를 사용하려 합니다.
- cache miss를 caller가 채우는 cache-aside인지, `CacheLoader`·`MapLoader`가 맡는 read-through인지 먼저 정합니다.
- `close()`가 entry를 지우지 않는다는 점과 Redisson client의 종료 책임이 애플리케이션에 있다는 점을 받아들입니다.

단일 JVM cache면 [cache-core](./bluetape4k-cache-core.md)가 더 작습니다. Redis 자료구조와 lock·topic까지 필요하면 [redisson](./bluetape4k-redisson.md) 매뉴얼도 함께 읽습니다.

## 의존성 추가 {#coordinates}

사용자는 Redisson이나 하위 라이브러리 버전을 따로 맞추지 않고 중앙 BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-redisson")
}
```

Gradle project path는 `:bluetape4k-cache-redisson`, source directory는 `cache/cache-redisson`입니다. 1.12.1 폴더 재편 뒤에도 Maven artifact와 `io.bluetape4k.cache` package는 그대로입니다.

## 첫 Redisson Near Cache {#quick-start}

Redisson client는 애플리케이션에서 만들고 종료합니다. Near Cache wrapper만 사용 범위에 맞춰 닫습니다.

```kotlin
import io.bluetape4k.cache.RedissonCaches
import io.bluetape4k.cache.nearcache.redissonNearCacheConfig
import java.time.Duration

val products = RedissonCaches.suspendNearCache<Product>(
    redisson = redissonClient,
    config = redissonNearCacheConfig {
        cacheName = "products"
        maxLocalSize = 5_000
        timeToLive = Duration.ofMinutes(30)
    },
)

products.put("p:1", Product("p:1", "keyboard"))
val product = products.get("p:1")
products.close()
```

기본 codec은 `RedissonCodecs.LZ4Fory`입니다. key는 `String`으로 고정됩니다. `close()`는 wrapper를 destroy하지만 `clearAll()`처럼 entry 전체 삭제를 뜻하지 않습니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| 표준 JCache | `RedissonJCaching`, `RedissonCaches.jcache` | lazy singleton `CacheManager`에서 같은 이름 cache를 재사용합니다. |
| coroutine JCache | `RedissonSuspendJCache`, `suspendJCache` | `*Async().await()`를 사용하지만 `getAndPut`은 get→put 두 단계입니다. |
| 동기 계산 재사용 | `RMap.memoizer` | evaluator는 호출 thread에서 실행되고 in-flight 공유는 JVM 내부입니다. |
| future 계산 재사용 | `RMap.asyncMemoizer` | cache 저장 실패는 경고하고 계산 결과 future는 정상 완료될 수 있습니다. |
| suspend 계산 재사용 | `RMap.suspendMemoizer` | evaluator 실패·취소는 저장하지 않고 다음 호출이 다시 계산합니다. |
| 동기 Near Cache | `RedissonCaches.nearCache` | blocking `RLocalCachedMap` 연산을 호출합니다. |
| suspend Near Cache | `RedissonCaches.suspendNearCache` | Redisson async 연산을 `await()`하며 `clearLocal()`은 local 호출입니다. |
| legacy front/back 조합 | `nearJCache`, `suspendNearJCache` | 새 코드는 native `RLocalCachedMap` 경로를 우선 검토합니다. |

## 학습 경로 {#concepts}

아래 장은 기능 목록보다 한 단계 더 들어갑니다. 1.12.1의 실제 source와 test를 따라가며 기본값, 경쟁 조건, 실패 뒤 상태와 운영 판단을 설명합니다. 예제를 실행한 뒤 source link에서 전체 구현을 확인할 수 있습니다.

1. [Redisson JCache와 suspend wrapper](./bluetape4k-cache-redisson/redisson-jcache-suspend.md) — provider 생성, cache 재사용, async bridge와 비원자적 `getAndPut`을 확인합니다.
2. [Memoizer와 같은 key 계산 병합](./bluetape4k-cache-redisson/memoizers-concurrency.md) — sync·future·suspend 경로의 JVM 내부 single-flight와 실패 복구를 비교합니다.
3. [RLocalCachedMap Near Cache와 무효화](./bluetape4k-cache-redisson/near-cache-invalidation.md) — local/Redis tier, Pub/Sub invalidation, clear 범위와 통계 한계를 설명합니다.
4. [설정, codec과 client 소유권](./bluetape4k-cache-redisson/config-codec-ownership.md) — TTL, idle, eviction, reconnect, wire format과 resource owner를 정합니다.
5. [실패, 취소, 수명주기와 운영](./bluetape4k-cache-redisson/failures-cancellation-operations.md) — Redis 장애, partial failure, cancellation, close와 관측 지표를 다룹니다.
6. [Cache 전략과 생태계 경로](./bluetape4k-cache-redisson/ecosystem-paths.md) — cache-aside와 read/write-through·behind를 구분하고 Hibernate, Spring, Exposed와 workshop으로 이어집니다.

처음 도입한다면 1→3→4 순서로 provider와 Near Cache 경계를 먼저 잡고, 계산 캐시가 필요할 때 2장을 읽습니다. 운영 중이라면 5장의 실패 표와 6장의 persistence 경로부터 확인해도 됩니다.

## 권장 패턴 {#patterns}

cache 이름은 서비스와 데이터 계약을 드러내게 정하고 환경 간 충돌을 피합니다. TTL과 local capacity를 명시하며 `INVALIDATE`와 reconnect `CLEAR` 기본값이 stale read 허용 범위에 맞는지 검토합니다.

memoizer evaluator는 재실행해도 안전한 조회·계산으로 제한합니다. 완료 값은 Redis에서 여러 JVM이 공유하지만 in-flight map은 process local이므로 서로 다른 JVM의 첫 miss는 evaluator를 각각 실행할 수 있습니다.

cache `put`을 database write-through라고 부르지 않습니다. persistence까지 연결하려면 loader·writer가 원본 저장소를 실제로 호출하는 별도 설계가 필요합니다.

## 연동 {#integrations}

이 모듈은 `bluetape4k-cache-core`의 API와 test fixture를 구현하고 `bluetape4k-redisson`의 client·codec 지원을 사용합니다. resilience4j는 Near Cache decorator 계약을 지원하기 위해 직접 연결됩니다.

[cache-core](./bluetape4k-cache-core.md)는 provider-independent 계약을, [redisson](./bluetape4k-redisson.md)은 더 넓은 Redisson API와 codec·future/coroutine bridge를 설명합니다. persistence cache는 [Hibernate](./bluetape4k-hibernate.md), [Spring Boot Hibernate Lettuce](./bluetape4k-spring-boot-hibernate-lettuce.md), Exposed workshop 경로에서 별도 transaction 규칙과 함께 다룹니다.

## 설정 {#configuration}

`RedissonNearCacheConfig`의 기본값은 cache name `redisson-near-cache`, local 최대 10,000개, TTL·idle 없음, `INVALIDATE`, reconnect `CLEAR`, LRU입니다. cache name은 blank일 수 없고 크기·TTL·idle은 양수여야 합니다.

wire format은 cache 생성 뒤 쉽게 바꿀 수 있는 UI 설정이 아닙니다. 기본 `LZ4Fory`와 다른 codec을 쓰려면 모든 producer·consumer가 같은 key/value format을 사용하도록 배포 순서를 정합니다.

JCache provider는 `META-INF/services/javax.cache.spi.CachingProvider`로 발견됩니다. 이름이 같은 cache가 이미 있으면 manager가 재사용하므로 테스트와 tenant마다 고유 이름을 씁니다.

## 실패 동작 {#failures}

`RedissonSuspendJCache.getAndPut`은 원자 연산이 아닙니다. 엄격한 원자성이 필요하면 Redisson `RMap`의 원자 API를 직접 선택합니다. suspend memoizer는 evaluator의 `CancellationException`을 성공 값으로 저장하지 않고 다시 던지며 in-flight entry를 제거합니다.

동기 Near Cache `close()`는 destroy 실패를 `runCatching`으로 소비합니다. suspend close는 일반 예외를 경고로 기록하지만 `CancellationException`은 전파합니다. 어느 경우에도 `close()`를 데이터 정리 API로 사용하지 않습니다.

Redis가 잠깐 불안정되었을 때 cache miss가 원본 저장소 부하로 이어질 수 있습니다. retry를 무작정 늘리면 cache가 장애 증폭기가 될 수 있으므로 timeout, fallback, 원본 pool 여유를 함께 정합니다.

## 운영 {#operations}

Redis latency·오류·reconnect와 local cache size, 통합 hit·miss를 함께 봅니다. 1.12.1의 native Near Cache 통계는 Redisson이 local/back hit를 분리해 노출하지 않아 `localHits`와 `localMisses`가 0이고, `backHits`/`backMisses`도 통합 조회 결과를 세는 값입니다. 이를 실제 Redis 왕복 횟수로 해석하면 안 됩니다.

codec decode 오류, cache name 충돌, local capacity와 eviction, Pub/Sub reconnect 뒤 clear 횟수를 운영 점검에 포함합니다. Redisson client 종료는 애플리케이션 lifecycle이 담당합니다.

## 테스트 {#testing}

모듈 테스트는 Redis Testcontainer를 사용하므로 병렬로 무겁게 실행하지 않습니다.

```bash
./gradlew :bluetape4k-cache-redisson:test --no-build-cache --no-configuration-cache
```

`RedissonCachesTest`와 `RedissonSuspendJCacheTest`는 factory·CRUD·close를, memoizer 테스트는 thread·virtual thread·coroutine 경합과 실패·취소 복구를 검증합니다. Near Cache 테스트는 `cache-core`의 공통 conformance fixture를 상속하며 resilient decorator도 별도로 확인합니다.

## 워크숍 {#workshops}

가장 작은 실행 예제는 이 모듈의 테스트입니다. persistence read-through, write-through, write-behind까지 보려면 [redisson-demo 매뉴얼](./bluetape4k-examples-redisson-demo.md)과 `examples/redisson-demo`의 cache strategy 테스트를 따라갑니다.

Exposed repository cache는 [exposed-workshop](https://github.com/bluetape4k/exposed-workshop)의 cache 장에서 `JdbcCacheRepository`, `EntityMapLoader`, `EntityMapWriter`로 확장합니다. 일반 Kotlin/Spring 예제는 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)에서 이어갈 수 있습니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 배포 소스를 기준으로 합니다. RESP3 hybrid Near Cache API는 이 모듈에 없습니다. native Redisson 경로는 `RLocalCachedMap`, legacy JCache 경로는 Caffeine front와 Redisson JCache back을 사용하므로 두 모델을 섞어 설명하지 않습니다.

memoizer의 in-flight 조율은 분산 lock이 아닙니다. native Near Cache 통계는 local/back을 정확히 분리하지 못합니다. JCache `getAndPut`의 coroutine wrapper도 원자성을 보장하지 않습니다.

## Source와 tests {#sources}

- [`RedissonCaches.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/RedissonCaches.kt)
- [`RedissonSuspendJCache.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/jcache/RedissonSuspendJCache.kt)
- [`RedissonMemoizer.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonMemoizer.kt)
- [`RedissonSuspendMemoizer.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizer.kt)
- [`RedissonNearCache.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCache.kt)
- [`RedissonNearCacheConfig.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheConfig.kt)
- [`RedissonSuspendMemoizerTest.kt`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizerTest.kt)
- [`RedissonNearCacheTest.kt`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheTest.kt)
