---
manualId: bluetape4k-cache-lettuce
title: "Lettuce JCache와 Near Cache"
description: "Redis JCache, 분산 memoizer와 Caffeine L1·Redis L2 Near Cache를 구성하고 무효화·TTL·수명주기를 검증하는 방법을 설명합니다."
kind: library
group: caching
learningOrder: 520
---

# Lettuce JCache와 Near Cache

## 제공하는 기능 {#problem}

`bluetape4k-cache-lettuce`는 `bluetape4k-cache-core`의 캐시 계약을 Lettuce와 Redis로 구현합니다. Redis hash 기반 JCache provider, 동기·suspend JCache, Redis에 결과를 공유하는 memoizer, Caffeine L1과 Redis L2를 묶는 Near Cache를 제공합니다.

Near Cache는 단순한 로컬 복제본이 아닙니다. `cacheName:key`로 Redis key 공간을 나누고, L1 miss를 Redis에서 채우며, RESP3 CLIENT TRACKING push로 다른 연결이 바꾼 key를 L1에서 지웁니다. tracking을 쓸 수 없는 환경에서도 읽기와 쓰기는 계속되지만 프로세스 간 L1 일관성은 별도 대책이 필요합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- JCache 표준 API가 필요한지, `NearCacheOperations`의 통계·L1/L2 API가 필요한지 고릅니다.
- 단일 JVM 캐시로 충분하다면 `bluetape4k-cache-core`의 Caffeine helper가 더 작습니다.
- JCache의 TTL은 entry별 TTL이 아니라 cache 이름에 해당하는 Redis hash 전체 TTL입니다.
- Near Cache의 Redis 값은 key마다 따로 저장되며 `redisTtl`도 각 Redis key에 적용됩니다.
- RESP3 CLIENT TRACKING을 쓴다면 Redis client를 RESP3로 구성하고 tracking 실패를 운영 지표로 잡습니다.
- codec은 이미 저장된 Redis byte format과 호환되어야 합니다. 변경할 때는 새 cache 이름이나 전체 비우기 경계를 둡니다.
- `RedisClient`, cache manager, cache connection 중 누가 무엇을 닫는지 먼저 정합니다.

## 의존성 추가 {#coordinates}

사용자는 Lettuce, Caffeine과 하위 bluetape4k 모듈 버전을 따로 맞추지 않고 중앙 BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-lettuce")
}
```

Redis 서버는 애플리케이션 실행 환경에서 준비합니다. 모듈의 Gradle project path는 `:bluetape4k-cache-lettuce`, source directory는 `cache/cache-lettuce`입니다.

## 첫 Near Cache {#quick-start}

RESP3 client와 명시적인 cache 이름, L1 용량, L1·L2 만료를 먼저 정합니다.

```kotlin
val redisClient = RedisClient.create("redis://localhost:6379").also {
    it.options = ClientOptions.builder()
        .protocolVersion(ProtocolVersion.RESP3)
        .build()
}

val users = LettuceCaches.nearCache<User>(redisClient) {
    cacheName = "users"
    maxLocalSize = 10_000
    frontExpireAfterWrite = Duration.ofMinutes(10)
    redisTtl = Duration.ofHours(1)
    useRespProtocol3 = true
    recordStats = true
}

try {
    users.put("42", user)
    check(users.get("42") == user)
} finally {
    users.close()
    redisClient.shutdown()
}
```

`put`은 Redis에 먼저 쓰고 성공하면 L1을 갱신합니다. 이는 두 캐시 계층 사이의 write-through이지 database까지 저장하는 repository write-through가 아닙니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| JCache SPI | `LettuceCachingProvider`, `LettuceCacheManager` | provider가 만든 manager는 RedisClient도 닫습니다. |
| 외부 RedisClient 재사용 | `LettuceJCaching` | manager는 외부 client를 닫지 않습니다. |
| 동기 JCache | `LettuceJCache`, `LettuceCaches.jcache` | 하나의 Redis hash에 저장하고 hash 전체 TTL을 갱신합니다. |
| coroutine JCache | `LettuceSuspendJCache`, `LettuceSuspendCacheManager` | blocking JCache 호출을 `Dispatchers.IO`에서 실행합니다. |
| 함수 결과 공유 | `LettuceMemoizer`, `LettuceAsyncMemoizer`, `LettuceSuspendMemoizer` | same-key 병합은 한 JVM의 `inFlight` map 범위입니다. |
| 동기 Near Cache | `LettuceNearCache`, `LettuceCaches.nearCache` | key type은 `String`; Redis write가 성공한 뒤 L1을 갱신합니다. |
| coroutine Near Cache | `LettuceSuspendNearCache` | Lettuce coroutine command와 async batch를 사용합니다. |
| JCache 기반 L1/L2 | `nearJCache`, `suspendNearJCache` | front/back 조합 계약은 `cache-core`가 제공합니다. |
| retry·fallback | `withResilience` | 별도 Lettuce 구현이 아니라 공통 decorator를 씌웁니다. |

## 학습 경로 {#concepts}

아래 장은 1.12.1 배포 소스와 실행 가능한 테스트를 따라 provider identity, Redis 저장 구조, 동시 계산, L1/L2 순서와 무효화 실패를 설명합니다. 예제마다 기본값과 소유권, 실패 뒤 상태를 함께 다루므로 첫 연결에서 운영 점검까지 순서대로 학습할 수 있습니다.

1. [JCache provider, manager와 설정](./bluetape4k-cache-lettuce/jcache-provider-manager.md) — SPI 탐색, `(ClassLoader, URI)` identity, Redis hash와 TTL·codec 설정을 확인합니다.
2. [동기·suspend JCache](./bluetape4k-cache-lettuce/sync-suspend-jcache.md) — CRUD, listener, EntryProcessor, IO dispatcher와 close/destroy 차이를 다룹니다.
3. [Redis memoizer와 동시 계산](./bluetape4k-cache-lettuce/memoizers-concurrency.md) — sync·future·suspend 경로, JVM 내 same-key 병합과 실패·취소 복구를 검증합니다.
4. [Near Cache L1·L2 동작](./bluetape4k-cache-lettuce/near-cache-l1-l2.md) — read fill, write 순서, key 격리, TTL, 통계와 bulk 연산을 설명합니다.
5. [RESP3 무효화와 Lua CAS](./bluetape4k-cache-lettuce/resp3-invalidation-lua.md) — CLIENT TRACKING 등록, push payload, `EVALSHA`와 `NOSCRIPT` fallback을 따라갑니다.
6. [수명주기, 테스트와 생태계 경로](./bluetape4k-cache-lettuce/operations-ecosystem.md) — 장애·종료·운영 점검에서 Hibernate, Spring, Exposed와 workshop으로 이어집니다.

처음 도입한다면 1→2 또는 1→4로 시작합니다. JCache 표준이 필요 없고 L1 hit·miss와 Redis 무효화가 목적이라면 4→5→6이 더 짧은 경로입니다.

## 권장 패턴 {#patterns}

cache 이름을 데이터 계약의 일부로 취급합니다. 이름, key encoding, value codec과 TTL을 함께 버전 관리하고 wire format을 바꿀 때 새 이름을 사용합니다. Near Cache에서는 source of truth 조회가 성공한 뒤 cache를 채우고, 데이터 변경 transaction이 끝난 뒤 관련 key를 갱신하거나 무효화합니다.

RESP3 invalidation은 요청 경로와 비동기로 도착합니다. 짧은 stale window도 허용할 수 없는 값은 Near Cache에 넣지 않거나 L1을 끕니다. Redis 장애 fallback을 추가할 때도 cache miss가 database 장애 증폭기로 바뀌지 않도록 pool과 query latency를 함께 제한합니다.

## 연동 {#integrations}

공통 JCache·Near Cache·resilience 계약은 [`bluetape4k-cache-core`](./bluetape4k-cache-core.md), Redis client·codec·`LettuceMap`은 [`bluetape4k-lettuce`](./bluetape4k-lettuce.md)에서 설명합니다. Hibernate 2차 캐시는 [`bluetape4k-hibernate-cache-lettuce`](./bluetape4k-hibernate-cache-lettuce.md), Spring Boot 자동 설정과 관측은 [`bluetape4k-spring-boot-hibernate-lettuce`](./bluetape4k-spring-boot-hibernate-lettuce.md)로 이어집니다.

database repository와 cache 전략을 함께 설계하려면 [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed), [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)과 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)의 cache 예제를 참고합니다. 일반 `cache.put`을 database write-through라고 부르지 않고 repository loader/writer 경계를 확인합니다.

## 설정 {#configuration}

`LettuceNearCacheConfig` 기본값은 cache 이름 `lettuce-near-cache`, L1 최대 10,000개, 쓰기 후 30분 만료, Redis TTL 없음, RESP3 tracking 사용, 통계 비활성입니다. cache 이름은 공백일 수 없고 용량과 duration은 0보다 커야 합니다.

`LettuceCacheConfig`의 `ttlSeconds`는 Redis hash 전체에 적용됩니다. 기본 key encoder는 `toString()`, 기본 value codec은 LZ4+Fory입니다. non-String key로 iterator를 사용하려면 역변환할 `keyDecoder`도 제공해야 합니다.

## 실패 동작 {#failures}

JCache manager는 중복 cache 이름과 닫힌 뒤 호출을 즉시 실패시킵니다. typed `getCache`의 key/value type이 설정과 다르면 `ClassCastException`이 발생합니다. codec 역직렬화 실패와 Redis command 오류는 숨기지 않습니다.

Near Cache에서 Redis write가 실패하면 L1을 갱신하기 전에 예외가 전파됩니다. RESP3 tracking 시작만 실패한 경우에는 경고를 남기고 cache가 계속 동작합니다. 이 경우 원격 변경으로 오래된 L1 값이 남을 수 있으므로 tracking 활성 여부를 별도 운영 계약으로 봅니다.

## 운영 {#operations}

L1 hit·miss·eviction과 크기, Redis hit·miss, command latency·error·reconnect를 함께 봅니다. `recordStats=false`이면 Caffeine hit·miss는 0으로 보일 수 있습니다. `backCacheSize`와 `clearAll`은 `cacheName:*`를 `SCAN`하고 삭제에는 `UNLINK`를 사용하므로 key 수가 많을 때 Redis 부하와 완료 시간을 측정합니다.

memoizer에서는 evaluator latency·failure와 hot key를, RESP3에서는 tracking 시작 실패와 invalidation 지연을 관찰합니다. Redis가 잠깐 불안정되었을 때 cache miss가 한꺼번에 원본 저장소로 몰리는지도 부하 테스트에 포함합니다.

## 테스트 {#testing}

모듈 테스트는 Redis Testcontainers를 사용하므로 다른 heavy database suite와 병렬로 실행하지 않습니다.

```bash
./gradlew :bluetape4k-cache-lettuce:test --no-build-cache --no-configuration-cache
```

`LettuceJCacheTest`는 TTL·typed key·EntryProcessor를, memoizer 테스트는 경쟁·실패·취소 뒤 재계산을 검증합니다. `LettuceNearCacheTrackingTest`는 외부 writer와 다른 cache instance의 무효화를, `LettuceNearCacheIsolationTest`는 `clearAll`이 다른 cache 이름을 침범하지 않는지 확인합니다.

## 워크숍 {#workshops}

모듈 내부의 `LettuceJCachesTest`부터 factory별 최소 예제를 실행할 수 있습니다. 이어서 `LettuceNearCacheTrackingTest`를 두 인스턴스 무효화 실습으로, `LettuceNearCacheIsolationTest`를 key namespace 실습으로 사용합니다.

database까지 연결한 cache-aside·read-through·write-through·write-behind 비교는 [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)과 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)에서 이어갑니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 release commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`의 1.12.1 소스와 테스트를 기준으로 합니다. JCache listener는 현재 프로세스의 cache instance가 직접 수행한 연산에서 발생하며 Redis Pub/Sub 기반 전역 JCache event bus가 아닙니다.

RESP3 tracking 시작 실패는 cache 시작을 중단하지 않습니다. `NOLOOP` 때문에 자기 connection의 쓰기는 push를 받지 않고 코드가 직접 L1을 갱신합니다. `withResilience`는 `cache-core`의 decorator이며 독립적인 `ResilientLettuceNearCache` 구현 클래스가 아닙니다.

## Source와 tests {#sources}

- [`LettuceCaches.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/LettuceCaches.kt)
- [`LettuceCachingProvider.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCachingProvider.kt)
- [`LettuceJCache.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCache.kt)
- [`LettuceSuspendJCache.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCache.kt)
- [`LettuceSuspendMemoizer.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceSuspendMemoizer.kt)
- [`LettuceNearCache.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt)
- [`TrackingInvalidationListener.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/TrackingInvalidationListener.kt)
- [`LettuceJCacheTest.kt`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceJCacheTest.kt)
- [`LettuceNearCacheTrackingTest.kt`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheTrackingTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### LettuceNearCache 클래스 계층도

[![LettuceNearCache 클래스 계층도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-01.svg)

_배포본 README: [`cache/cache-lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-lettuce/README.md)_

### Lettuce JCache NearCache 구조도

[![Lettuce JCache NearCache 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-02.svg)

_배포본 README: [`cache/cache-lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-lettuce/README.ko.md)_

### Lettuce Cache Stability Contracts 다이어그램

[![Lettuce Cache Stability Contracts 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-03.svg)

_배포본 README: [`cache/cache-lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-lettuce/README.ko.md)_

### Native Lettuce NearCache 구조도

[![Native Lettuce NearCache 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-04.svg)

_배포본 README: [`cache/cache-lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-lettuce/README.ko.md)_

### RESP3 CLIENT TRACKING Invalidation 다이어그램

[![RESP3 CLIENT TRACKING Invalidation 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-sequence-01.svg)

_배포본 README: [`cache/cache-lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-lettuce/README.ko.md)_

<!-- release-readme-diagrams:end -->
