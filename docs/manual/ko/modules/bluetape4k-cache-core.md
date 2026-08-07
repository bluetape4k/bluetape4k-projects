---
manualId: bluetape4k-cache-core
title: "캐시 추상화와 메모이제이션"
description: "로컬 캐시, JCache, memoizer와 2단계 Near Cache의 공통 계약을 선택하고 실패·동시성·수명주기 경계를 검증하는 방법을 설명합니다."
kind: library
group: caching
learningOrder: 500
---

# 캐시 추상화와 메모이제이션

## 제공하는 기능 {#problem}

`bluetape4k-cache-core`는 캐시 provider를 고르기 전에 필요한 공통 계약을 제공합니다. Caffeine·Cache2k·Ehcache helper, JCache 생성과 설정, coroutine용 `SuspendJCache`, sync·async·suspend memoizer, 그리고 로컬 front와 원격 back을 묶는 Near Cache 인터페이스가 한 모듈에 들어 있습니다.

이 모듈을 쓰면 캐시를 단순한 `Map`이 아니라 **누가 miss를 채우는지, 같은 key의 동시 계산을 어떻게 합치는지, 어느 계층을 비우는지, backend 실패를 전파할지**가 드러나는 API로 다룰 수 있습니다. 다만 cache entry는 원본 데이터가 아닙니다. `put`은 캐시 계층 사이에 값을 복제할 뿐이며 database까지 갱신하는 진짜 write-through를 뜻하지 않습니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 단일 JVM의 반복 계산·조회라면 local provider나 memoizer 중 더 작은 API를 고릅니다.
- coroutine 경로에는 `SuspendJCache` 또는 suspend memoizer를 사용하고 blocking cache 호출을 감싸서 non-blocking이라고 부르지 않습니다.
- miss를 caller가 채우는 cache-aside인지, JCache `CacheLoader`가 맡는 read-through인지 분명히 합니다.
- 최대 entry 수와 expiry를 명시합니다. 기본 JCache 설정은 eternal이므로 그대로 두면 시간 기반 만료가 없습니다.
- 여러 JVM이 값을 공유하거나 local invalidation이 필요하면 `cache-lettuce`나 `cache-redisson` 같은 provider 모듈로 넘어갑니다.
- cache miss가 한꺼번에 몰릴 수 있는 key에는 memoizer의 same-key 병합 또는 provider loader의 동시성 계약을 검토합니다.

## 의존성 추가 {#coordinates}

사용자는 Caffeine, Cache2k, Ehcache와 JCache의 세부 버전을 맞추지 않고 중앙 BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-core")
}
```

Cache2k와 Ehcache 본체는 1.12.1에서 `compileOnly`입니다. 해당 helper를 사용한다면 애플리케이션 runtime에 선택한 provider를 추가합니다. Caffeine과 Caffeine JCache provider는 API dependency로 들어옵니다.

## 첫 로컬 캐시 {#quick-start}

용량과 만료를 정한 Caffeine cache를 만들고, miss는 caller가 채웁니다.

```kotlin
import io.bluetape4k.cache.caffeine.caffeine
import java.time.Duration

val users = caffeine {
    maximumSize(10_000)
    expireAfterWrite(Duration.ofMinutes(10))
    recordStats()
}.build<String, User>()

fun findUser(id: String): User {
    users.getIfPresent(id)?.let { return it }
    return userRepository.findById(id).also { loaded -> users.put(id, loaded) }
}
```

이 코드는 cache-aside입니다. repository 저장을 cache API가 수행하지 않으므로 write-through가 아닙니다. 데이터를 바꾼 뒤에는 같은 transaction 경계에서 cache를 갱신하거나 무효화하는 규칙을 별도로 정해야 합니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| Caffeine 구성 | `caffeine`, `caffeineSpecOf`, `suspendLoadingCache` | capacity, expiry, stats는 builder에서 직접 켭니다. |
| Cache2k 구성 | `cache2k`, `getOrCreateCache2k` | provider 본체는 runtime에 애플리케이션이 제공합니다. |
| Ehcache 구성 | `ehcacheManager`, `getOrCreateCache` | manager는 `ShutdownQueue`에 등록되며 기본 helper는 off-heap 32 MB를 포함합니다. |
| JCache 표준 API | `jcacheConfiguration`, `jcacheManager`, `JCaching` | 기본 expiry는 eternal이고 provider manager는 lazy singleton입니다. |
| coroutine cache | `SuspendJCache`, `CaffeineSuspendJCache` | API가 `suspend`여도 provider 구현의 I/O 방식은 따로 확인합니다. |
| 함수 결과 재사용 | `Memoizer`, `AsyncMemoizer`, `SuspendMemoizer` | same-key 병합, 실패 제거, `clear()` 세대를 테스트합니다. |
| 2단계 cache 계약 | `NearCacheOperations`, `SuspendNearCacheOperations` | key는 `String`; 실제 invalidation은 provider 구현이 맡습니다. |
| retry·fallback | `withResilience`, `NearCacheResilienceConfig` | 읽기 fallback과 쓰기 예외 정책이 다릅니다. |

## 학습 경로 {#concepts}

아래 장은 README의 기능 목록을 늘어놓지 않습니다. 1.12.1 소스와 실행 가능한 테스트를 따라가며 실제 기본값, 동시성 조건, 실패 뒤 상태와 provider 선택 기준을 설명합니다. 각 장의 예제는 source·test 링크와 함께 제공하므로 API를 처음 쓰는 개발자도 작은 실험부터 운영 점검까지 이어갈 수 있습니다.

1. [로컬 provider와 JCache](./bluetape4k-cache-core/local-providers-jcache.md) — Caffeine·Cache2k·Ehcache helper, JCache manager와 expiry 기본값을 비교합니다.
2. [Cache-aside와 loader·writer 계약](./bluetape4k-cache-core/cache-aside-loader-writer.md) — caller가 miss를 채우는 패턴과 JCache read/write-through 구성을 구분합니다.
3. [Memoizer와 같은 key 동시 계산](./bluetape4k-cache-core/memoizers-single-flight.md) — sync·future·suspend 계산, `SingleFlight`, 실패와 `clear()` 경계를 다룹니다.
4. [Near Cache의 front·back 동작](./bluetape4k-cache-core/near-cache-semantics.md) — local hit, back lookup, front fill, 무효화와 통계를 순서대로 확인합니다.
5. [Retry, 실패와 수명주기](./bluetape4k-cache-core/resilience-failures-lifecycle.md) — 읽기 fallback, 쓰기 예외, coroutine 취소와 close 동작을 검증합니다.
6. [테스트와 생태계 경로](./bluetape4k-cache-core/testing-ecosystem-paths.md) — 공통 test fixture에서 Lettuce·Redisson, Exposed와 workshop까지 이어집니다.

처음 도입한다면 1→2→3 순서로 로컬 계약을 잡고 4→5에서 분산 cache 경계를 배웁니다. 이미 provider를 쓰고 있다면 5장의 장애 표와 6장의 conformance test부터 확인해도 됩니다.

## 권장 패턴 {#patterns}

local cache는 작은 용량과 짧은 expiry로 시작하고 hit rate뿐 아니라 load 횟수와 eviction을 함께 측정합니다. cache-aside loader는 실패한 값을 저장하지 않고, 같은 key의 동시 miss가 backend를 압박한다면 memoizer나 loading cache로 계산 병합을 검토합니다.

쓰기에서는 원본 저장 성공과 cache 변경의 순서를 문서화합니다. cache `put`을 database write-through라고 부르지 말고, 원본 저장 뒤 invalidate 또는 갱신이 실패했을 때 어떤 stale window를 허용하는지 정합니다.

## 연동 {#integrations}

`bluetape4k-cache-lettuce`와 `bluetape4k-cache-redisson`은 이 모듈의 공통 Near Cache·suspend·resilience 계약 위에 Redis backend를 올립니다. [bluetape4k-cache-lettuce](./bluetape4k-cache-lettuce.md)는 Lettuce와 RESP3 기반 invalidation을, [bluetape4k-cache-redisson](./bluetape4k-cache-redisson.md)은 Redisson local cached map 경로를 설명합니다.

database repository 캐싱은 별도 계층입니다. JDBC·Exposed repository에서 진짜 read-through, write-through, write-behind를 비교하려면 6장의 `JdbcCacheRepository`와 workshop 경로를 따릅니다.

## 설정 {#configuration}

모듈 전체를 제어하는 properties 파일은 없습니다. capacity·expiry·statistics는 provider builder 또는 JCache `MutableConfiguration`에서 지정합니다. `getDefaultJCacheConfiguration()`은 `EternalExpiryPolicy`를 사용합니다. `NearJCacheConfig`의 Caffeine front cache는 접근 후 30분 만료가 기본이며, 원격 동기화 timeout 기본값은 500 ms입니다.

resilience 기본값은 최대 시도 3회, 첫 대기 500 ms, exponential backoff 사용, 읽기 실패 시 `RETURN_FRONT_OR_NULL`입니다. `retryMaxAttempts`와 `retryWaitDuration`은 0보다 커야 합니다.

## 실패 동작 {#failures}

`Cache.getOrPut`은 동시 miss에서 supplier를 둘 이상 실행할 수 있습니다. `putIfAbsent`로 최종 cache 값은 맞추지만 외부 I/O 중복 실행까지 막지는 않습니다. side effect가 있거나 backend 비용이 큰 계산은 memoizer의 same-key 병합 계약을 사용합니다.

resilience decorator는 read 계열이 재시도에 실패하면 전략에 따라 `null`, 빈 map, `false`를 반환하거나 예외를 전파합니다. write·remove 계열은 재시도 후에도 실패하면 예외를 전파합니다. suspend decorator는 `CancellationException`을 retry하거나 fallback으로 바꾸지 않고 즉시 다시 던집니다.

## 운영 {#operations}

용량, hit·miss, eviction, evaluator 실행 횟수와 load latency를 함께 봅니다. Near Cache에서는 local hit·miss와 back hit·miss를 나눠 보고 Redis 오류와 원본 저장소 latency를 같은 시간축에 둡니다. cache 장애 때 원본 저장소로 요청이 몰릴 수 있으므로 pool saturation과 timeout도 경보에 포함합니다.

manager와 cache의 소유자를 하나로 정합니다. `CaffeineSuspendJCache.close()`는 local entry를 비우고 cleanup을 호출하며, Ehcache·JCache provider helper는 `ShutdownQueue`에 등록됩니다. 테스트마다 고유 cache 이름을 쓰고 종료 시 clear 또는 close해 전역 manager에 상태가 남지 않게 합니다.

## 테스트 {#testing}

local provider와 공통 계약만 검증하므로 외부 Redis 없이 실행할 수 있습니다.

```bash
./gradlew :bluetape4k-cache-core:test --no-build-cache --no-configuration-cache
```

`CaffeineSuspendJCacheTest`는 suspend CRUD를, `SingleFlightTest`와 provider별 memoizer test는 동시 계산·실패 복구·clear를 검증합니다. `ResilientNearCacheDecoratorTest`와 suspend 대응 테스트는 retry, fallback, cancellation, close 실패를 고정합니다. 새 provider는 공개 test fixture를 상속해 같은 계약을 다시 검증합니다.

## 워크숍 {#workshops}

모듈 안의 테스트가 가장 작은 실행 예제입니다. 이어서 [cache-lettuce 매뉴얼](./bluetape4k-cache-lettuce.md)과 [cache-redisson 매뉴얼](./bluetape4k-cache-redisson.md)에서 실제 Redis invalidation을 실습할 수 있습니다.

database 연동 전략은 [exposed-workshop](https://github.com/bluetape4k/exposed-workshop)의 cache 장과 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)의 cache 예제로 확장합니다. 이 경로에서는 `JdbcCacheRepository`, `EntityMapLoader`, `EntityMapWriter`를 사용해 단순 cache `put`과 persistence write-through를 구분합니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 배포 소스를 기준으로 합니다. `cache-core`는 Redis server, cluster invalidation, persistence transaction을 직접 제공하지 않습니다. 그런 기능은 provider나 repository 계층이 맡습니다.

`SuspendJCache`는 비동기 모양의 공통 계약이지 모든 구현이 자동으로 non-blocking임을 보증하는 표식이 아닙니다. legacy `NearJCache`와 새 `NearCacheOperations` 계열도 동작 차이가 있으므로 새 코드는 provider 매뉴얼이 권하는 factory와 공통 conformance test를 우선합니다.

## Source와 tests {#sources}

- [`CaffeineSupport.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupport.kt)
- [`JCacheSupport.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheSupport.kt)
- [`CaffeineSuspendJCache.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/CaffeineSuspendJCache.kt)
- [`SingleFlight.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/memoizer/SingleFlight.kt)
- [`NearCacheOperations.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheOperations.kt)
- [`ResilientSuspendNearCacheDecorator.kt`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecorator.kt)
- [`JCacheSupportExtTest.kt`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheSupportExtTest.kt)
- [`SingleFlightTest.kt`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/memoizer/SingleFlightTest.kt)
- [`ResilientSuspendNearCacheDecoratorTest.kt`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecoratorTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### NearCache Interface Hierarchy 다이어그램

[![NearCache Interface Hierarchy 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-01.svg)

_배포본 README: [`cache/cache-core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.md)_

### NearCacheOperations (Blocking) 다이어그램

[![NearCacheOperations (Blocking) 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-02.svg)

_배포본 README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

### SuspendNearCacheOperations (Coroutine) 다이어그램

[![SuspendNearCacheOperations (Coroutine) 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-03.svg)

_배포본 README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

### SuspendJCache coroutine interface 다이어그램

[![SuspendJCache coroutine interface 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-04.svg)

_배포본 README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

### NearJCache synchronous two-tier cache 다이어그램

[![NearJCache synchronous two-tier cache 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-05.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-05.svg)

_배포본 README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

### SuspendNearJCache coroutine operation 지도 다이어그램

[![SuspendNearJCache coroutine operation 지도 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-06.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-diagram-06.svg)

_배포본 README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

### NearCache get() (front miss → back lookup → front fill) 다이어그램

[![NearCache get() (front miss → back lookup → front fill) 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-sequence-01.svg)

_배포본 README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

### NearCache put() (write-through) 다이어그램

[![NearCache put() (write-through) 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-core-sequence-02.svg)

_배포본 README: [`cache/cache-core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-core/README.ko.md)_

<!-- release-readme-diagrams:end -->
