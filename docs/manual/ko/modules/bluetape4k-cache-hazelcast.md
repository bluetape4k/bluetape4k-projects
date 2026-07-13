---
manualId: bluetape4k-cache-hazelcast
title: "Module bluetape4k-cache-hazelcast"
description: "bluetape4k-cache-hazelcast는 Hazelcast 기반 JCache Provider, Coroutines 캐시 구현, 그리고 Caffeine + Hazelcast IMap 2-Tier Near Cache를 제공합니다."
kind: library
group: caching
---

# Module bluetape4k-cache-hazelcast

## 해결하는 문제 {#problem}

bluetape4k-cache-hazelcast는 Hazelcast 기반 JCache Provider, Coroutines 캐시 구현, 그리고 Caffeine + Hazelcast IMap 2-Tier Near Cache를 제공합니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 cache key, consistency, invalidation, backend ownership이 필요할 때 `bluetape4k-cache-hazelcast`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-hazelcast")
}
```

Gradle project path는 `:bluetape4k-cache-hazelcast`, source directory는 `cache/cache-hazelcast`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `HazelcastCaches`, `HazelcastJCaching`, `HazelcastSuspendJCache`, `HazelcastAsyncMemoizer`, `HazelcastMemoizer`, `HazelcastSuspendMemoizer`, `HazelcastEntryEventListener`, `HazelcastLocalCache`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`HazelcastCaches`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`HazelcastCaches`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HazelcastJCaching`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/jcache/HazelcastJCaching.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HazelcastSuspendJCache`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/jcache/HazelcastSuspendJCache.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HazelcastAsyncMemoizer`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastAsyncMemoizer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HazelcastMemoizer`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastMemoizer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HazelcastSuspendMemoizer`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastSuspendMemoizer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HazelcastEntryEventListener`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastEntryEventListener.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HazelcastLocalCache`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastLocalCache.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HazelcastNearCache`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCache.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HazelcastNearCacheConfig`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCacheConfig.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

loading 계약을 하나로 명확히 선택합니다. **캐시 어사이드(cache-aside)**에서는 caller가 miss를 처리해 값을 load하고 cache에 다시 씁니다. **read-through**에서는 cache loader가 miss 경로를 소유합니다. **write-through**에서는 cache API가 성공을 반환하기 전에 backing store까지 write를 전파합니다. 구현에 이 계약이 없다면 일반 `put`을 write-through라고 부르지 않습니다. 2단계 Near Cache는 L1을 먼저 읽고 miss이면 L2를 조회한 뒤 L1을 채웁니다. 구현이 정한 순서대로 L2와 L1을 write 또는 invalidate하고, backend update 실패 뒤 오래된 L1 값이 남지 않는지 partial failure test로 확인합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-cache-core"))
api(libs.hazelcast)
implementation(libs.resilience4j.retry)
implementation(libs.resilience4j.kotlin)
compileOnly(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

모듈에서 찾은 설정 resource는 다음과 같습니다.

- [`javax.cache.spi.CachingProvider`](../../../../cache/cache-hazelcast/src/main/resources/META-INF/services/javax.cache.spi.CachingProvider)

override하기 전에 이 resource와 binding source에서 property 이름과 default를 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

hit ratio, load latency, eviction, stale read, backend 오류, reconnect 동작을 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-cache-hazelcast:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractHazelcastTest`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/AbstractHazelcastTest.kt)
- [`HazelcastCachesTest`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/HazelcastCachesTest.kt)
- [`HazelcastServers`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/HazelcastServers.kt)
- [`HazelcastSuspendJCacheTest`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/jcache/HazelcastSuspendJCacheTest.kt)
- [`HazelcastAsyncMemoizerTest`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/memoizer/HazelcastAsyncMemoizerTest.kt)
- [`HazelcastMemoizerTest`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/memoizer/HazelcastMemoizerTest.kt)
- [`HazelcastSuspendMemoizerTest`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/memoizer/HazelcastSuspendMemoizerTest.kt)
- [`AbstractHazelcastNearCacheTest`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/AbstractHazelcastNearCacheTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../cache/cache-hazelcast/README.ko.md)
- [모듈 build](../../../../cache/cache-hazelcast/build.gradle.kts)
- [`HazelcastCaches`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt)
- [`HazelcastJCaching`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/jcache/HazelcastJCaching.kt)
- [`HazelcastSuspendJCache`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/jcache/HazelcastSuspendJCache.kt)
- [`HazelcastAsyncMemoizer`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastAsyncMemoizer.kt)
- [`HazelcastMemoizer`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastMemoizer.kt)
- [`HazelcastSuspendMemoizer`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastSuspendMemoizer.kt)
- [`HazelcastEntryEventListener`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastEntryEventListener.kt)
- [`HazelcastLocalCache`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastLocalCache.kt)
- [`HazelcastNearCache`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCache.kt)
- [`HazelcastNearCacheConfig`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCacheConfig.kt)
- [`AbstractHazelcastTest`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/AbstractHazelcastTest.kt)
- [`HazelcastCachesTest`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/HazelcastCachesTest.kt)
