---
manualId: bluetape4k-cache-redisson
title: "Module bluetape4k-cache-redisson"
description: "English | 한국어"
kind: library
group: caching
---

# Module bluetape4k-cache-redisson

## 해결하는 문제 {#problem}

English | 한국어 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 cache key, consistency, invalidation, backend ownership이 필요할 때 `bluetape4k-cache-redisson`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-redisson")
}
```

Gradle project path는 `:bluetape4k-cache-redisson`, source directory는 `cache/cache-redisson`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `RedissonCaches`, `RedissonJCaching`, `RedissonSuspendJCache`, `RedissonAsyncMemoizer`, `RedissonMemoizer`, `RedissonSuspendMemoizer`, `RedissonNearCache`, `RedissonNearCacheConfig`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`RedissonCaches`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/RedissonCaches.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`RedissonCaches`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/RedissonCaches.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonJCaching`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/jcache/RedissonJCaching.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonSuspendJCache`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/jcache/RedissonSuspendJCache.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonAsyncMemoizer`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonAsyncMemoizer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonMemoizer`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonMemoizer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonSuspendMemoizer`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonNearCache`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCache.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonNearCacheConfig`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheConfig.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonSuspendNearCache`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonSuspendNearCache.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **패키지 / import 안정성**, **제공 API**, **Near-Cache Capability**, **의존성**, **권장 사용 시나리오**, **Anti-Patterns**, **예제**, **Suspend JCache**, **Native Redisson Near Cache**, **Suspend Memoizer** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-cache-core"))
api(libs.redisson)
api(project(":bluetape4k-redisson"))
implementation(libs.resilience4j.retry)
implementation(libs.resilience4j.kotlin)
implementation(project(":bluetape4k-coroutines"))
implementation(libs.kotlinx.coroutines.core)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

모듈에서 찾은 설정 resource는 다음과 같습니다.

- [`javax.cache.spi.CachingProvider`](../../../../cache/cache-redisson/src/main/resources/META-INF/services/javax.cache.spi.CachingProvider)

override하기 전에 이 resource와 binding source에서 property 이름과 default를 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

hit ratio, load latency, eviction, stale read, backend 오류, reconnect 동작을 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-cache-redisson:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`RedisServers`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/RedisServers.kt)
- [`RedissonCachesTest`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/RedissonCachesTest.kt)
- [`RedissonSuspendJCacheTest`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/jcache/RedissonSuspendJCacheTest.kt)
- [`RedissonAsyncMemoizerTest`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/memoizer/RedissonAsyncMemoizerTest.kt)
- [`RedissonMemoizerTest`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/memoizer/RedissonMemoizerTest.kt)
- [`RedissonSuspendMemoizerTest`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizerTest.kt)
- [`RedissonNearCacheConfigTest`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheConfigTest.kt)
- [`RedissonNearCacheTest`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../cache/cache-redisson/README.ko.md)
- [모듈 build](../../../../cache/cache-redisson/build.gradle.kts)
- [`RedissonCaches`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/RedissonCaches.kt)
- [`RedissonJCaching`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/jcache/RedissonJCaching.kt)
- [`RedissonSuspendJCache`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/jcache/RedissonSuspendJCache.kt)
- [`RedissonAsyncMemoizer`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonAsyncMemoizer.kt)
- [`RedissonMemoizer`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonMemoizer.kt)
- [`RedissonSuspendMemoizer`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizer.kt)
- [`RedissonNearCache`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCache.kt)
- [`RedissonNearCacheConfig`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheConfig.kt)
- [`RedissonSuspendNearCache`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonSuspendNearCache.kt)
- [`RedisServers`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/RedisServers.kt)
- [`RedissonCachesTest`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/RedissonCachesTest.kt)
- [`RedissonSuspendJCacheTest`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/jcache/RedissonSuspendJCacheTest.kt)
