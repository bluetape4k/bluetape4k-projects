---
manualId: bluetape4k-probabilistic
title: "확률적 자료 구조"
description: "JVM 애플리케이션에서 사용할 인메모리 확률적 자료구조 모듈입니다."
kind: library
group: utilities
learningOrder: 1260
---

# 확률적 자료 구조

## 해결하는 문제 {#problem}

JVM 애플리케이션에서 사용할 인메모리 확률적 자료구조 모듈입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 입력 계약, value semantics, algorithm cost, deterministic output이 필요할 때 `bluetape4k-probabilistic`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-probabilistic")
}
```

Gradle project path는 `:bluetape4k-probabilistic`, source directory는 `utils/probabilistic`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `BloomFilter`, `BloomFilterConfig`, `BloomFilters`, `BloomHasher`, `InMemoryBloomFilter`, `InMemoryMutableBloomFilter`, `InMemorySuspendBloomFilter`, `MutableBloomFilter`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`BloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilter.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`BloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilter.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`BloomFilterConfig`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilterConfig.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`BloomFilters`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilters.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`BloomHasher`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomHasher.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`InMemoryBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryBloomFilter.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`InMemoryMutableBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryMutableBloomFilter.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`InMemorySuspendBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemorySuspendBloomFilter.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MutableBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/MutableBloomFilter.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SuspendBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/SuspendBloomFilter.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **Gradle**, **Bloom Filter**, **Coroutine API**, **주의 사항** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-core"))
compileOnly(libs.kotlinx.coroutines.core)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

hot path를 측정하고 입력 크기를 제한하며 utility를 호출하는 application boundary에서 failure를 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-probabilistic:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`BloomFilterConfigTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilterConfigTest.kt)
- [`InMemoryBloomFilterTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryBloomFilterTest.kt)
- [`InMemorySuspendBloomFilterTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemorySuspendBloomFilterTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../utils/probabilistic/README.ko.md)
- [모듈 build](../../../../utils/probabilistic/build.gradle.kts)
- [`BloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilter.kt)
- [`BloomFilterConfig`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilterConfig.kt)
- [`BloomFilters`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilters.kt)
- [`BloomHasher`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomHasher.kt)
- [`InMemoryBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryBloomFilter.kt)
- [`InMemoryMutableBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryMutableBloomFilter.kt)
- [`InMemorySuspendBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemorySuspendBloomFilter.kt)
- [`MutableBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/MutableBloomFilter.kt)
- [`SuspendBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/SuspendBloomFilter.kt)
- [`BloomFilterConfigTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilterConfigTest.kt)
- [`InMemoryBloomFilterTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryBloomFilterTest.kt)
- [`InMemorySuspendBloomFilterTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemorySuspendBloomFilterTest.kt)
