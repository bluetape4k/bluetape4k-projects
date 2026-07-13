---
manualId: bluetape4k-redisson
title: "bluetape4k-redisson"
description: "Redisson Redis 클라이언트를 Kotlin에서 편리하게 사용할 수 있도록 확장한 모듈입니다. DSL 방식의 클라이언트 생성, 고성능 Codec, Kotlin Coroutines 지원, NearCache 기능을 제공합니다."
kind: library
group: infrastructure
---

# bluetape4k-redisson

## 해결하는 문제 {#problem}

Redisson Redis 클라이언트를 Kotlin에서 편리하게 사용할 수 있도록 확장한 모듈입니다. DSL 방식의 클라이언트 생성, 고성능 Codec, Kotlin Coroutines 지원, NearCache 기능을 제공합니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 client lifecycle, reconnect policy, backpressure, retry, observability이 필요할 때 `bluetape4k-redisson`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-redisson")
}
```

Gradle project path는 `:bluetape4k-redisson`, source directory는 `infra/redisson`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `RStreamSupport`, `RedissonClientExtensions`, `RedissonClientSupport`, `RedissonConst`, `CacheInvalidationStrategy`, `LocalCacheMapSupport`, `MapCacheSupport`, `RedissonCacheConfig`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`RStreamSupport`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RStreamSupport.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`RStreamSupport`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RStreamSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonClientExtensions`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientExtensions.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonClientSupport`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonConst`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonConst.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CacheInvalidationStrategy`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/CacheInvalidationStrategy.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LocalCacheMapSupport`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/LocalCacheMapSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MapCacheSupport`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/MapCacheSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonCacheConfig`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/RedissonCacheConfig.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`FastForyCodec`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCodec.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`Fastjson2Codec`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Fastjson2Codec.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **주요 기능**, **의존성**, **아키텍처 다이어그램**, **Codec 선택 맵**, **NearCache 2-Tier 캐시 흐름**, **Batch / Transaction 처리 흐름**, **사용 예시**, **1. RedissonClient 생성**, **DSL 방식**, **YAML 설정 파일 방식** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
api(project(":bluetape4k-io"))
api(project(":bluetape4k-netty"))
api(libs.redisson)
compileOnly(libs.redisson.spring.boot.starter)
compileOnly(project(":bluetape4k-cache-core"))
compileOnly(project(":bluetape4k-idgenerators"))
compileOnly(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
compileOnly(libs.kotlinx.coroutines.reactor)
compileOnly(libs.fory.kotlin)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

connection 상태, queue 깊이, retry, timeout, remote 오류, graceful shutdown을 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-redisson:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractRedissonTest`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/AbstractRedissonTest.kt)
- [`RStreamSupportTest`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RStreamSupportTest.kt)
- [`RedissonClientExtensionsTest`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonClientExtensionsTest.kt)
- [`RedissonClientSupportTest`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupportTest.kt)
- [`RedissonTestUtils`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonTestUtils.kt)
- [`RedissonConcurrencyBenchmark`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/benchmark/RedissonConcurrencyBenchmark.kt)
- [`CacheInvalidationStrategyTest`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/cache/CacheInvalidationStrategyTest.kt)
- [`LocalCacheMapSupportTest`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/cache/LocalCacheMapSupportTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../infra/redisson/README.ko.md)
- [모듈 build](../../../../infra/redisson/build.gradle.kts)
- [`RStreamSupport`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RStreamSupport.kt)
- [`RedissonClientExtensions`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientExtensions.kt)
- [`RedissonClientSupport`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt)
- [`RedissonConst`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonConst.kt)
- [`CacheInvalidationStrategy`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/CacheInvalidationStrategy.kt)
- [`LocalCacheMapSupport`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/LocalCacheMapSupport.kt)
- [`MapCacheSupport`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/MapCacheSupport.kt)
- [`RedissonCacheConfig`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/RedissonCacheConfig.kt)
- [`FastForyCodec`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCodec.kt)
- [`Fastjson2Codec`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Fastjson2Codec.kt)
- [`AbstractRedissonTest`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/AbstractRedissonTest.kt)
- [`RStreamSupportTest`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RStreamSupportTest.kt)
