---
manualId: bluetape4k-redisson
title: "Redisson 코루틴 확장"
description: "Redisson client, 분산 객체와 Stream, coroutine 경계, Codec, Near Cache와 cache persistence를 1.12.1 소스에 맞춰 설명합니다."
kind: library
group: caching
learningOrder: 560
---

# Redisson 코루틴 확장

## 제공하는 기능 {#problem}

`bluetape4k-redisson`은 Redisson을 Kotlin 애플리케이션에서 구성하고 운용할 때 반복되는 코드를 줄입니다. DSL·YAML 기반 client 생성, batch와 transaction, `RFuture`의 coroutine adapter, Stream consumer-group helper, Codec 조합, `RMapCache`와 `RLocalCachedMap` 구성을 제공합니다.

이 모듈이 Redis client의 수명주기나 데이터 일관성을 대신 결정하지는 않습니다. 생성한 `RedissonClient`는 애플리케이션이 종료하고, Near Cache의 동기화 전략과 Codec wire format도 배포 단위가 함께 관리해야 합니다. `MapLoader`나 `MapWriter`를 연결하지 않은 `get`/`put` 코드는 cache-aside일 뿐 read-through·write-through가 아닙니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- client를 Spring Boot가 소유할지 직접 생성하고 `shutdown()`할지 정합니다.
- 동기 API, `RFuture`, coroutine 중 서비스 경계에 맞는 한 가지 호출 모델을 고릅니다.
- Redis 값이 한 배포 안에서만 쓰이는지, 여러 서비스·버전이 공유하는지에 따라 Codec을 선택합니다.
- 로컬 캐시의 stale 허용 시간과 Pub/Sub 무효화가 끊겼다가 복구될 때의 정책을 정합니다.
- cache-aside가 필요한지, `MapLoader`/`MapWriter`를 붙인 read/write-through 또는 write-behind가 필요한지 구분합니다.
- Stream consumer group의 ACK, pending message claim, 재처리·중복 처리 규칙을 애플리케이션에서 정합니다.

## 의존성 추가 {#coordinates}

사용자는 개별 라이브러리 버전을 맞추지 않고 `bluetape4k-dependencies` BOM 버전만 관리합니다. Codec과 coroutine 확장은 `compileOnly` 의존성이 있으므로 실제로 쓰는 기능의 runtime dependency를 애플리케이션에 추가합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-redisson")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core") // coroutine API 사용 시
}
```

## 첫 client와 map {#quick-start}

client를 직접 만들었다면 같은 component가 종료 책임도 집니다.

```kotlin
import io.bluetape4k.redis.redisson.redissonClient
import io.bluetape4k.redis.redisson.cache.mapCache
import java.time.Duration

val redisson = redissonClient {
    useSingleServer().address = "redis://127.0.0.1:6379"
}

val users = mapCache<String, String>("users", redisson) {
    timeToLive(Duration.ofMinutes(10))
}

try {
    users.put("42", "Debop")
} finally {
    redisson.shutdown()
}
```

`mapCache`는 `RMapCache`를 만드는 DSL입니다. 이 예제의 `put`은 Redis cache에만 기록하므로 cache-aside입니다. DB 쓰기까지 자동화하려면 5장에서 loader/writer 경계를 설계합니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| client 생성 | `redissonClient`, `redissonClientOf`, `configFromYamlOf` | 호출마다 새 client를 만들며 종료하지 않습니다. |
| 많은 동시 연결 기본값 | `applyHighConcurrencyDefaults` | CPU 기반 기본값일 뿐 Redis·애플리케이션 용량 검증을 대신하지 않습니다. |
| batch·transaction | `withBatch`, `withTransaction` | rollback 실패는 원래 예외를 덮지 않습니다. coroutine에서는 suspend 버전을 씁니다. |
| future·coroutine 변환 | `sequence`, `awaitAll`, `withSuspendedBatch`, `withSuspendedTransaction` | 실패·cancellation을 삼키지 말고 호출 scope로 전파합니다. |
| 분산 객체·Stream | Redisson `RMap`, `RLock`, `RStream`; `ackAllAsync`, `claimAllAsync` | group·consumer 이름과 ID 목록을 검증하며 중복 처리 정책은 호출자가 소유합니다. |
| TTL map | `mapCache` | entry TTL을 지원하지만 DB persistence를 제공하지 않습니다. |
| local cached map | `localCachedMap`, `RedissonNearCache` | sync/reconnection 정책과 `destroy()` 시점을 정해야 합니다. |
| cache persistence | `RedissonCacheConfig.toMapOptions`, `toLocalCachedMapOptions` | 실제 read/write-through에는 별도 loader/writer가 필요합니다. |
| Codec | `RedissonCodecs`, `Jackson3Codec`, `Fastjson2Codec`, `GzipCodec` | wire compatibility, allow-list, 압축 해제 상한을 배포 계약으로 관리합니다. |

## 학습 경로 {#concepts}

아래 장은 API 목록을 반복하지 않습니다. 1.12.1 배포 소스와 대표 테스트를 바탕으로 ownership, failure, 일관성 경계를 설명하고 바로 적용할 수 있는 예제와 확인할 source link를 함께 제공합니다.

1. [Client와 분산 객체·Stream](./bluetape4k-redisson/client-distributed-objects-streams.md) — client ownership, batch·transaction, lock ID와 consumer-group helper를 익힙니다.
2. [Future와 coroutine 경계](./bluetape4k-redisson/future-coroutine-boundaries.md) — `RFuture`, `CompletableFuture`, suspend API의 실패와 cancellation 전달을 확인합니다.
3. [Codec과 보안](./bluetape4k-redisson/codecs-security-wire-format.md) — Fory·JSON·압축 Codec의 wire format, allow-list와 migration 위험을 정리합니다.
4. [Local Cached Map과 무효화](./bluetape4k-redisson/local-cache-pubsub-invalidation.md) — JVM front cache, Redis back cache, Pub/Sub 동기화와 reconnect 정책을 다룹니다.
5. [Cache mode와 DB persistence](./bluetape4k-redisson/cache-modes-persistence.md) — cache-aside와 진짜 read/write-through·write-behind를 loader/writer 경계로 구분합니다.
6. [Lifecycle·테스트·생태계 경로](./bluetape4k-redisson/lifecycle-testing-ecosystem.md) — 종료, 장애 관찰, Testcontainers, cache-redisson·Exposed·workshop으로 이어지는 선택을 설명합니다.

처음 도입한다면 1→2→3→4 순서로 읽고, DB와 cache를 함께 갱신해야 한다면 5장을 반드시 읽습니다. 기존 cache abstraction이나 workshop과 연결하려면 6장에서 시작합니다.

## 권장 패턴 {#patterns}

client는 process 또는 Spring application context 단위로 한 번 만들고 종료 hook에서 `shutdown()`합니다. Redis 명령을 모으려면 batch를 쓰되 원자성이 필요하면 transaction을 선택합니다. coroutine code에서는 async 명령과 `await()`를 사용하고 blocking `get()`을 섞지 않습니다.

Near Cache는 단순한 성능 switch가 아닙니다. stale 허용 시간, `SyncStrategy`, `ReconnectionStrategy`, cache name과 Codec을 모든 node에서 맞춘 뒤 사용합니다. DB persistence가 필요하면 cache write와 DB write를 같은 말로 뭉개지 말고 cache-aside, write-through, write-behind를 명확히 구분합니다.

## 연동 {#integrations}

Redisson client 자체는 API dependency입니다. Spring Boot starter, coroutine, cache-core, idgenerators, Fory·Jackson·Fastjson2·압축 라이브러리는 선택 기능입니다. 사용한 Codec이 요구하는 runtime dependency가 빠지면 client 생성 또는 첫 encode 시점에 실패할 수 있습니다.

Spring Cache abstraction이 필요하면 [`bluetape4k-cache-redisson`](./bluetape4k-cache-redisson.md)을 사용합니다. DB entity를 Exposed repository와 함께 cache하려면 5·6장의 외부 repository 경로를 따릅니다.

## 설정 {#configuration}

`configFromYamlOf`는 InputStream, String, File, URL의 Redisson YAML을 읽고 지정한 Codec을 덮어씁니다. `applyHighConcurrencyDefaults`는 thread와 connection pool, timeout, retry delay를 CPU 기반 값으로 설정합니다. 운영 환경에서는 Redis topology와 command latency를 측정한 뒤 조정합니다.

`RedissonCacheConfig`는 음수 TTL·크기·retry 값을 거부합니다. `toMapOptions`와 `toLocalCachedMapOptions`는 지원하지 않는 `ttl`, `maxSize`, `deleteFromDBOnInvalidate`가 설정되면 조용히 무시하지 않고 `IllegalArgumentException`으로 실패합니다.

## 실패 동작 {#failures}

동기·suspend transaction은 action 실패 시 rollback을 시도하고 원래 예외를 다시 던집니다. rollback 실패는 원래 실패를 덮지 않지만 suspend rollback에서 발생한 `CancellationException`은 보존합니다. Stream helper는 빈 group·consumer 이름이나 빈 ID 목록을 즉시 거부합니다.

Codec decode 실패를 무조건 fallback으로 복구하면 신뢰 경계가 넓어집니다. `Jackson3Codec`과 `Fastjson2Codec`에 package allow-list를 지정하면 binary fallback decode가 기본적으로 차단됩니다. 마이그레이션 때문에 열어야 한다면 제한된 기간과 trusted Redis data에만 적용합니다.

## 운영 {#operations}

Redis 연결 수, command latency와 timeout, retry, reconnect, batch 크기, transaction rollback, Stream pending entry, Near Cache hit ratio와 stale incident를 함께 관찰합니다. Pub/Sub 연결이 끊겼을 때 local cache가 어떤 reconnection policy로 복구되는지도 장애 훈련에 포함합니다.

write-behind는 DB 반영 지연과 유실 가능성을 운영 계약에 추가합니다. queue backlog, writer failure, 재시도 횟수와 drain 시간을 별도 metric으로 두고, 종료 전에 pending write가 처리되는지 확인합니다.

## 테스트 {#testing}

1.12.1 대표 테스트는 실제 Redis를 Testcontainers로 시작해 client, Stream, cache, coroutine과 Codec 계약을 검증합니다.

```bash
./gradlew :bluetape4k-redisson:test --no-build-cache --no-configuration-cache
```

이 작업은 Docker를 사용하는 heavy test이므로 다른 Testcontainers suite와 병렬 실행하지 않습니다. 매뉴얼 수정만 할 때는 source link와 문서 구조 검증으로 충분하며, API 동작을 바꿀 때 위 task를 실행합니다.

## 워크숍 {#workshops}

모듈 안에서는 `RedissonClientSupportTest`, `RStreamSupportTest`, `RedissonClientCoroutineTest`, `LocalCacheMapSupportTest`, `RedissonNearCacheTest`가 가장 작은 실행 예제입니다. cache와 DB를 함께 다루는 실습은 [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)의 cache chapter와 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)의 Redis 예제로 이어갑니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 배포 commit을 기준으로 합니다. `RedissonNearCache`는 `RLocalCachedMap`에 위임하며 별도의 두 map을 수동으로 일관되게 갱신하는 abstraction이 아닙니다. `destroy()`는 local near-cache instance만 정리하고 Redis data는 남깁니다.

`RedissonCacheConfig`의 preset 이름만으로 DB read/write-through가 생기지 않습니다. `MapLoader`와 `MapWriter` 구현을 options에 실제로 연결해야 하며, `deleteFromDBOnInvalidate`는 1.12.1 option 변환에서 지원하지 않습니다.

## Source와 tests {#sources}

- [`RedissonClientSupport.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt)
- [`RedissonClientExtensions.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientExtensions.kt)
- [`RStreamSupport.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RStreamSupport.kt)
- [`RedissonClientCoroutine.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/coroutines/RedissonClientCoroutine.kt)
- [`RedissonCacheConfig.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/RedissonCacheConfig.kt)
- [`RedissonNearCache.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/nearcache/RedissonNearCache.kt)
- [`Jackson3Codec.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Jackson3Codec.kt)
- [`RedissonClientCoroutineTest.kt`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/coroutines/RedissonClientCoroutineTest.kt)
- [`RedissonNearCacheTest.kt`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/nearcache/RedissonNearCacheTest.kt)
- [`RedissonCacheConfigTest.kt`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/cache/RedissonCacheConfigTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Codec 선택 맵 다이어그램

[![Codec 선택 맵 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-diagram-01.svg)

_배포본 README: [`infra/redisson/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/redisson/README.ko.md)_

### Batch / Transaction 다이어그램

[![Batch / Transaction 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-diagram-02.svg)

_배포본 README: [`infra/redisson/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/redisson/README.ko.md)_

### NearCache 2-Tier 다이어그램

[![NearCache 2-Tier 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-sequence-01.svg)

_배포본 README: [`infra/redisson/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/redisson/README.ko.md)_

<!-- release-readme-diagrams:end -->
