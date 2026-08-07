---
manualId: bluetape4k-bucket4j
title: "분산 요청 속도 제한"
description: "Bucket4j 기반으로 애플리케이션 레벨 Rate Limiter를 구성하기 위한 래퍼/유틸 모듈입니다."
kind: library
group: operations
learningOrder: 1010
---

# 분산 요청 속도 제한

## 해결하는 문제 {#problem}

Bucket4j 기반으로 애플리케이션 레벨 Rate Limiter를 구성하기 위한 래퍼/유틸 모듈입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 client lifecycle, reconnect policy, backpressure, retry, observability이 필요할 때 `bluetape4k-bucket4j`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-bucket4j")
}
```

Gradle project path는 `:bluetape4k-bucket4j`, source directory는 `infra/bucket4j`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `BucketKeyValidation`, `ConfigurationSupport`, `SuspendLocalBucket`, `AsyncBucketProxyProvider`, `BucketProxyProvider`, `LettuceBasedProxyManagerSupport`, `RedissonBasedProxyManagerSupport`, `Slf4jBucketListener`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`BucketKeyValidation`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/BucketKeyValidation.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`BucketKeyValidation`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/BucketKeyValidation.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ConfigurationSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/ConfigurationSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SuspendLocalBucket`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/coroutines/SuspendLocalBucket.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`AsyncBucketProxyProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/AsyncBucketProxyProvider.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`BucketProxyProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/BucketProxyProvider.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceBasedProxyManagerSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/redis/LettuceBasedProxyManagerSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonBasedProxyManagerSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/redis/RedissonBasedProxyManagerSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`Slf4jBucketListener`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/internal/Slf4jBucketListener.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`AbstractLocalBucketProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/local/AbstractLocalBucketProvider.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LocalBucketProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/local/LocalBucketProvider.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **주요 기능**, **클래스 구조**, **Bucket4j 통합 클래스 다이어그램**, **Rate Limiting 시퀀스 다이어그램**, **로컬 Rate Limiter — 토큰 소비 흐름**, **분산 Suspend Rate Limiter — Redis 기반 코루틴 흐름**, **Bucket4j 직접 사용 대비 추가 기능**, **의존성 추가**, **사용 예**, **1) Local Rate Limiter** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
compileOnly(project(":bluetape4k-cache-core"))
api(libs.bucket4j.core)
compileOnly(libs.bucket4j.lettuce)
compileOnly(libs.bucket4j.redisson)
compileOnly(libs.caffeine)
compileOnly(libs.lettuce.core)
compileOnly(libs.redisson)
compileOnly(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
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
./gradlew :bluetape4k-bucket4j:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractBucket4jTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/AbstractBucket4jTest.kt)
- [`ConfigurationSupportTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/ConfigurationSupportTest.kt)
- [`TestRedisServer`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/TestRedisServer.kt)
- [`SuspendLocalBucketListenerTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/coroutines/SuspendLocalBucketListenerTest.kt)
- [`SuspendedLocalBucketTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/coroutines/SuspendedLocalBucketTest.kt)
- [`AbstractAsyncBucketProxyProviderTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/distributed/AbstractAsyncBucketProxyProviderTest.kt)
- [`AbstractBucketProxyProviderTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/distributed/AbstractBucketProxyProviderTest.kt)
- [`LettuceAsyncBucketProxyProviderTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/distributed/redis/LettuceAsyncBucketProxyProviderTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Bucket4j 통합 클래스 다이어그램

[![Bucket4j 통합 클래스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-diagram-01.svg)

_배포본 README: [`infra/bucket4j/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/bucket4j/README.ko.md)_

### 로컬 Rate Limiter 토큰 소비 흐름

[![로컬 Rate Limiter 토큰 소비 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-sequence-01.svg)

_배포본 README: [`infra/bucket4j/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/bucket4j/README.ko.md)_

### 분산 Suspend Rate Limiter Redis 코루틴 흐름

[![분산 Suspend Rate Limiter Redis 코루틴 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-sequence-02.svg)

_배포본 README: [`infra/bucket4j/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/bucket4j/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../infra/bucket4j/README.ko.md)
- [모듈 build](../../../../infra/bucket4j/build.gradle.kts)
- [`BucketKeyValidation`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/BucketKeyValidation.kt)
- [`ConfigurationSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/ConfigurationSupport.kt)
- [`SuspendLocalBucket`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/coroutines/SuspendLocalBucket.kt)
- [`AsyncBucketProxyProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/AsyncBucketProxyProvider.kt)
- [`BucketProxyProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/BucketProxyProvider.kt)
- [`LettuceBasedProxyManagerSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/redis/LettuceBasedProxyManagerSupport.kt)
- [`RedissonBasedProxyManagerSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/redis/RedissonBasedProxyManagerSupport.kt)
- [`Slf4jBucketListener`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/internal/Slf4jBucketListener.kt)
- [`AbstractLocalBucketProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/local/AbstractLocalBucketProvider.kt)
- [`LocalBucketProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/local/LocalBucketProvider.kt)
- [`AbstractBucket4jTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/AbstractBucket4jTest.kt)
- [`ConfigurationSupportTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/ConfigurationSupportTest.kt)
