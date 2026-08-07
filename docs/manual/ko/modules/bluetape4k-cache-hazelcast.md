---
manualId: bluetape4k-cache-hazelcast
title: "Hazelcast 캐시 통합"
description: "Hazelcast JCache와 IMap 기반 memoizer·Near Cache를 구성하고 클러스터 소유권, 무효화, 직렬화와 실패 경계를 검증하는 방법을 설명합니다."
kind: library
group: caching
learningOrder: 510
---

# Hazelcast 캐시 통합

## 제공하는 기능 {#problem}

`bluetape4k-cache-hazelcast`는 이미 연결된 `HazelcastInstance`를 JCache, 함수 결과 memoizer, Caffeine L1과 Hazelcast `IMap` L2를 묶은 Near Cache로 사용할 수 있게 합니다. 동기 API와 `CompletableFuture`, coroutine API를 함께 제공하며 `IMap` entry event로 각 JVM의 L1을 무효화합니다.

이 모듈은 Hazelcast cluster를 만들거나 소유하지 않습니다. 연결, 인증, member discovery, backup, map TTL과 직렬화는 애플리케이션의 Hazelcast 설정이 맡고, 이 모듈은 전달받은 instance와 map 위에 캐시 계약을 얹습니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 표준 JCache가 필요한지, `IMap`을 직접 쓰는 Near Cache·memoizer가 필요한지 고릅니다.
- 단일 JVM 캐시라면 `bluetape4k-cache-core`의 Caffeine helper가 더 작습니다.
- Redis cache가 아니라 Hazelcast data grid와 같은 key 공간을 공유해야 할 때 선택합니다.
- Near Cache key는 `String`으로 고정됩니다. memoizer와 JCache는 generic key를 받습니다.
- value가 cluster의 Hazelcast serialization 설정으로 읽고 쓸 수 있는지 먼저 확인합니다.
- L1의 짧은 stale window를 허용할 수 있는지, listener 등록·해제와 cluster 장애를 어떻게 관측할지 정합니다.
- `HazelcastInstance`를 시작하고 종료할 소유자는 애플리케이션입니다.

## 의존성 추가 {#coordinates}

사용자는 Hazelcast와 하위 bluetape4k 모듈 버전을 따로 맞추지 않고 중앙 BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-hazelcast")
}
```

Gradle project path는 `:bluetape4k-cache-hazelcast`, source directory는 `cache/cache-hazelcast`입니다. Hazelcast cluster와 client/member 설정은 애플리케이션 실행 환경에서 준비합니다.

## 첫 Near Cache {#quick-start}

```kotlin
val hazelcast: HazelcastInstance = applicationHazelcastClient()

val users = HazelcastCaches.nearCache<User>(hazelcast) {
    cacheName = "users-v1"
    maxLocalSize = 10_000
    frontExpireAfterWrite = Duration.ofMinutes(10)
    recordStats = true
}

try {
    users.put("42", user)
    check(users.get("42") == user)
} finally {
    users.close()       // listener와 Caffeine L1 정리
    // hazelcast.shutdown()은 애플리케이션 소유자가 수행
}
```

`put`은 L1을 먼저 바꾼 뒤 `IMap.set`을 호출합니다. backend write가 실패하면 이미 바뀐 L1이 남을 수 있으므로 이 순서를 database write-through나 원자적 dual write로 해석하면 안 됩니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| Hazelcast JCache | `HazelcastJCaching`, `HazelcastCaches.jcache` | 전달받은 instance로 manager를 찾으며 SPI 자동 탐색을 쓰지 않습니다. |
| coroutine JCache | `HazelcastSuspendJCache` | `ICache` async API를 우선하고 나머지는 `Dispatchers.IO`에서 실행합니다. |
| 함수 결과 공유 | `HazelcastMemoizer`, `AsyncHazelcastMemoizer`, `SuspendHazelcastMemoizer` | same-key 계산 병합은 한 JVM의 `inFlight` map 범위입니다. |
| 동기 Near Cache | `HazelcastNearCache` | Caffeine L1과 String-key `IMap` L2를 조합합니다. |
| coroutine Near Cache | `HazelcastSuspendNearCache` | 일부 bulk·조건부 명령은 `Dispatchers.IO`, async 명령은 `await`를 사용합니다. |
| JCache 기반 L1/L2 | `HazelcastCaches.nearJCache`, `suspendNearJCache` | factory는 listener 직렬화 실패를 피하려고 listener 없이 구성합니다. |
| retry·fallback | `withResilience` | `cache-core`의 decorator이며 Hazelcast 전용 write-behind 구현은 아닙니다. |

## 학습 경로 {#concepts}

아래 장은 1.12.1 배포 소스와 실행 가능한 테스트를 따라 instance 소유권, async 경계, 동시 계산, L1/L2 순서와 listener 직렬화 실패를 설명합니다. 각 장에서 정상 경로뿐 아니라 실패 뒤 어느 계층이 바뀌어 있는지도 확인합니다.

1. [JCache와 HazelcastInstance 소유권](./bluetape4k-cache-hazelcast/jcache-instance-ownership.md) — provider 선택, manager 생성, cache 이름과 instance 수명주기를 정리합니다.
2. [Suspend JCache와 async 경계](./bluetape4k-cache-hazelcast/suspend-jcache-async-boundaries.md) — `ICache` unwrap, `await`, IO fallback과 비원자적 `getAndPut`을 다룹니다.
3. [IMap memoizer와 동시 계산](./bluetape4k-cache-hazelcast/imap-memoizers-concurrency.md) — sync·future·suspend 실행 위치, JVM 내 병합과 cluster 경쟁을 설명합니다.
4. [IMap Near Cache와 무효화](./bluetape4k-cache-hazelcast/imap-near-cache-invalidation.md) — Caffeine L1, `IMap` L2, entry listener, write 순서와 통계를 따라갑니다.
5. [JCache Near Cache와 직렬화 제한](./bluetape4k-cache-hazelcast/jcache-near-cache-serialization.md) — listener factory 직렬화 실패와 listener-free factory의 일관성 한계를 확인합니다.
6. [설정, 장애, 테스트와 생태계](./bluetape4k-cache-hazelcast/operations-testing-ecosystem.md) — 설정 validation, 종료, 운영 점검과 backend 선택 기준을 정리합니다.

JCache가 목적이면 1→2→5, native Near Cache가 목적이면 4→6, 함수 결과 공유가 목적이면 3→6 순서로 읽습니다.

## 권장 패턴 {#patterns}

cache 이름과 value serialization schema를 하나의 배포 계약으로 관리합니다. value 형식을 바꿀 때는 cluster의 serializer 호환성을 확인하고 필요하면 새 map/cache 이름으로 전환합니다. 원본 저장소를 읽은 뒤 cache를 채우고, database 변경이 commit된 뒤 관련 key를 갱신하거나 지웁니다.

Near Cache는 원격 변경 이벤트가 도착할 때까지 잠시 오래된 값을 돌려줄 수 있습니다. 이 시간이 허용되지 않는 값은 L1에 두지 않습니다. Hazelcast 장애 fallback을 붙일 때도 miss가 원본 저장소 장애 증폭기가 되지 않도록 동시 evaluator 수와 원본 조회 제한을 함께 둡니다.

## 연동 {#integrations}

공통 JCache·Near Cache·resilience 계약은 [`bluetape4k-cache-core`](./bluetape4k-cache-core.md)에서 설명합니다. Redis가 운영 표준이라면 RESP3 invalidation을 쓰는 [`bluetape4k-cache-lettuce`](./bluetape4k-cache-lettuce.md)와 Redisson 분산 객체를 쓰는 [`bluetape4k-cache-redisson`](./bluetape4k-cache-redisson.md)을 비교합니다.

Hazelcast는 `IMap`과 entry event를 이미 쓰는 data-grid 애플리케이션에 자연스럽습니다. cache operation을 database write-through라고 부르기 전에 repository loader/writer 경계를 별도로 설계하고, [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)와 [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)의 database cache 전략을 이어서 봅니다.

## 설정 {#configuration}

`HazelcastNearCacheConfig` 기본값은 이름 `hazelcast-near-cache`, L1 최대 10,000개, 쓰기 후 30분 만료, 접근 후 만료 없음, 통계 비활성입니다. 이름은 공백일 수 없고 용량과 duration은 0보다 커야 합니다.

이 설정은 Caffeine L1만 구성합니다. `IMap`의 backup, TTL, max-idle, in-memory format과 serializer는 Hazelcast `Config` 또는 `ClientConfig`에서 별도로 관리합니다. 모듈은 독자적인 value codec이나 wire format을 추가하지 않습니다.

## 실패 동작 {#failures}

native Near Cache의 `put`, `putAll`, `remove`는 L1을 먼저 바꿉니다. 이어지는 `IMap` 호출이 실패하면 로컬 상태와 cluster 상태가 다를 수 있습니다. `replace`와 성공한 `putIfAbsent`는 backend 결과를 확인한 뒤 L1을 갱신합니다. `getAndRemove`와 `getAndReplace`는 여러 호출을 조합하므로 단일 원자 연산이 아닙니다.

JCache Near Cache의 직접 listener-backed factory는 Caffeine front cache를 담은 listener factory를 cluster로 직렬화하다 `HazelcastSerializationException`으로 실패합니다. `HazelcastCaches` factory는 listener를 빼서 read/write는 가능하지만 peer L1 무효화를 보장하지 않습니다.

## 운영 {#operations}

L1 hit·miss·eviction·size, IMap hit·miss, entry event 지연, client reconnect와 operation latency를 함께 봅니다. `recordStats=false`이면 Caffeine hit·miss는 0으로 보일 수 있습니다. memoizer에서는 evaluator latency·failure·hot key와 같은 key의 cluster 중복 계산을 관찰합니다.

`close`는 Near Cache listener와 L1을 정리할 뿐 `IMap` 데이터나 `HazelcastInstance`를 종료하지 않습니다. `clearAll`은 공유 `IMap` 전체를 비우므로 cache 이름을 다른 기능과 재사용하지 않습니다.

## 테스트 {#testing}

모듈 테스트는 Hazelcast Testcontainers를 사용하므로 다른 heavy database suite와 병렬로 실행하지 않습니다.

```bash
./gradlew :bluetape4k-cache-hazelcast:test --no-build-cache --no-configuration-cache
```

Near Cache 테스트는 CRUD, bulk, 통계와 중복 close를, memoizer 테스트는 same-key 경쟁과 실패 뒤 재계산을 검증합니다. JCache Near Cache 테스트는 listener-backed 생성이 직렬화 오류로 실패하고 factory의 listener-free read/write가 동작하는 현재 capability를 고정합니다.

## 워크숍 {#workshops}

`HazelcastCachesTest`에서 factory별 최소 생성을, `HazelcastNearCacheTest`와 `HazelcastSuspendNearCacheTest`에서 L1 miss·bulk·clear 경계를 실습할 수 있습니다. `HazelcastNearJCacheTest`는 성공 예제가 아니라 지원하지 않는 listener 조합을 확인하는 회귀 테스트도 포함합니다.

database cache-aside와 loader/writer를 함께 실습하려면 [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)과 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)으로 이어갑니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 release commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`의 1.12.1 소스와 테스트를 기준으로 합니다. `META-INF/services/javax.cache.spi.CachingProvider`에는 provider class가 등록되지 않았으며 `HazelcastJCaching`이 `HazelcastCachingProvider`를 명시적으로 생성합니다.

README에 남아 있는 `ResilientHazelcastNearCache`와 전용 write-behind queue 구현은 이 release source에 없습니다. resilience가 필요하면 `cache-core`의 `withResilience` decorator 범위에서 사용합니다. factory가 만든 JCache Near Cache는 listener-free degraded mode이며 peer front-cache propagation을 제공하지 않습니다.

## Source와 tests {#sources}

- [`HazelcastCaches.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt)
- [`HazelcastJCaching.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/jcache/HazelcastJCaching.kt)
- [`HazelcastSuspendJCache.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/jcache/HazelcastSuspendJCache.kt)
- [`HazelcastMemoizer.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastMemoizer.kt)
- [`HazelcastNearCache.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCache.kt)
- [`HazelcastEntryEventListener.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastEntryEventListener.kt)
- [`HazelcastNearJCacheTest.kt`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCacheTest.kt)
- [`HazelcastSuspendNearCacheTest.kt`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/HazelcastSuspendNearCacheTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### HazelcastNearCache 클래스 계층도

[![HazelcastNearCache 클래스 계층도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-diagram-01.svg)

_배포본 README: [`cache/cache-hazelcast/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-hazelcast/README.ko.md)_

### HazelcastNearCache 런타임 처리 흐름

[![HazelcastNearCache 런타임 처리 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-diagram-02.svg)

_배포본 README: [`cache/cache-hazelcast/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-hazelcast/README.ko.md)_

### IMap EntryListener Invalidation 다이어그램

[![IMap EntryListener Invalidation 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-sequence-01.svg)

_배포본 README: [`cache/cache-hazelcast/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-hazelcast/README.ko.md)_

<!-- release-readme-diagrams:end -->
