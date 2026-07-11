---
manualId: bluetape4k-cache-core
title: "Module bluetape4k-cache-core"
description: "English | 한국어"
kind: library
group: caching
---

# Module bluetape4k-cache-core

## 해결하는 문제 {#problem}

English | 한국어 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 cache key, consistency, invalidation, backend ownership이 필요할 때 `bluetape4k-cache-core`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-core")
}
```

Gradle project path는 `:bluetape4k-cache-core`, source directory는 `cache/cache-core`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `Cache2kSupport`, `CaffeineSupport`, `EhcacheSupport`, `CaffeineSuspendJCache`, `JCacheEntryEventListener`, `JCacheSupport`, `JCacheType`, `JCaching`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`Cache2kSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupport.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`Cache2kSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CaffeineSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`EhcacheSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/ehcache/EhcacheSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CaffeineSuspendJCache`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/CaffeineSuspendJCache.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`JCacheEntryEventListener`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheEntryEventListener.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`JCacheSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`JCacheType`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheType.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`JCaching`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCaching.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SuspendJCache`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCache.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SuspendJCacheEntry`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntry.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **패키지 / import 안정성**, **제공 기능**, **설치**, **제공 기능 (상세)**, **Near-Cache Capability Matrix**, **NearCache 통일 인터페이스**, **Coroutine 취소와 retry**, **Suspend Memoizer 실패 복구**, **NearCache get() 동작 시퀀스 (front miss → back lookup → front fill)**, **NearCache put() 동작 시퀀스 (write-through)** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-io"))
api(project(":bluetape4k-idgenerators"))
api(libs.javax.cache.api)
api(libs.caffeine)
api(libs.caffeine.jcache)
compileOnly(libs.cache2k.core)
compileOnly(libs.cache2k.jcache)
compileOnly(libs.ehcache)
compileOnly(libs.ehcache.clustered)
compileOnly(libs.ehcache.transactions)
implementation(libs.resilience4j.retry)
implementation(libs.resilience4j.kotlin)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

hit ratio, load latency, eviction, stale read, backend 오류, reconnect 동작을 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-cache-core:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`Cache2kSupportExtTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupportExtTest.kt)
- [`Cache2kSupportTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupportTest.kt)
- [`CaffeineSupportTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupportTest.kt)
- [`EhcacheSupportTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/ehcache/EhcacheSupportTest.kt)
- [`CaffeineSuspendJCacheTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/CaffeineSuspendJCacheTest.kt)
- [`JCacheEntryEventListenerTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheEntryEventListenerTest.kt)
- [`JCacheReadWriteThroughExample`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheReadWriteThroughExample.kt)
- [`JCacheSupportExtTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheSupportExtTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../cache/cache-core/README.ko.md)
- [모듈 build](../../../../cache/cache-core/build.gradle.kts)
- [`Cache2kSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupport.kt)
- [`CaffeineSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupport.kt)
- [`EhcacheSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/ehcache/EhcacheSupport.kt)
- [`CaffeineSuspendJCache`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/CaffeineSuspendJCache.kt)
- [`JCacheEntryEventListener`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheEntryEventListener.kt)
- [`JCacheSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheSupport.kt)
- [`JCacheType`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheType.kt)
- [`JCaching`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCaching.kt)
- [`SuspendJCache`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCache.kt)
- [`SuspendJCacheEntry`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntry.kt)
- [`Cache2kSupportExtTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupportExtTest.kt)
- [`Cache2kSupportTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupportTest.kt)
