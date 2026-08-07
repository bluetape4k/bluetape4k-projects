---
manualId: bluetape4k-vertx
title: "Vert.x 코루틴 확장"
description: "Vert.x 기반 비동기/Coroutines 개발을 위한 단일 통합 모듈입니다."
kind: library
group: io
learningOrder: 450
---

# Vert.x 코루틴 확장

## 해결하는 문제 {#problem}

Vert.x 기반 비동기/Coroutines 개발을 위한 단일 통합 모듈입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 encoding boundary, resource ownership, streaming, 호환성, malformed input이 필요할 때 `bluetape4k-vertx`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-vertx")
}
```

Gradle project path는 `:bluetape4k-vertx`, source directory는 `io/vertx`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `CoroutineSupport`, `FutureExtensions`, `VertxSupport`, `VertxDecorators`, `VertxFutureBulkheadSupport`, `VertxFutureCircuitBreakerSupport`, `VertxFutureRateLimiterSupport`, `VertxFutureRetrySupport`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`CoroutineSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/CoroutineSupport.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`CoroutineSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/CoroutineSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`FutureExtensions`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/FutureExtensions.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VertxSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/VertxSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VertxDecorators`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxDecorators.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VertxFutureBulkheadSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureBulkheadSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VertxFutureCircuitBreakerSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureCircuitBreakerSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VertxFutureRateLimiterSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureRateLimiterSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VertxFutureRetrySupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureRetrySupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VertxFutureSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VertxFutureTimeLimiterSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureTimeLimiterSupport.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **제공 기능**, **Vert.x Core (구 vertx/core)**, **Vert.x SQL Client (구 vertx/sqlclient)**, **Resilience4j 통합 (구 vertx/resilience4j)**, **아키텍처 다이어그램**, **모듈 의존성 구조**, **Vert.x 이벤트 루프 + Coroutines 처리 흐름**, **Circuit Breaker + Resilience4j 통합 흐름**, **Vert.x 핵심 컴포넌트 클래스 구조**, **설치** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-io"))
api(project(":bluetape4k-netty"))
api(project(":bluetape4k-coroutines"))
api(project(":bluetape4k-jdbc"))
api(libs.vertx.core)
api(libs.vertx.lang.kotlin)
api(libs.vertx.lang.kotlin.coroutines)
compileOnly(libs.vertx.web)
compileOnly(libs.vertx.web.client)
compileOnly(libs.vertx.junit5)
api(project(":bluetape4k-resilience4j"))
compileOnly(libs.resilience4j.reactor)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

payload 크기, allocation, latency, malformed input 비율, resource close, protocol 오류를 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-vertx:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractVertxTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/AbstractVertxTest.kt)
- [`SampleVerticle`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/SampleVerticle.kt)
- [`VertxSupportTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/VertxSupportTest.kt)
- [`LifecycleExamples`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/examples/LifecycleExamples.kt)
- [`SampleVerticleTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/examples/SampleVerticleTest.kt)
- [`VertxJunit5Examples`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/examples/VertxJunit5Examples.kt)
- [`AbstractVertxFutureTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/resilience4j/AbstractVertxFutureTest.kt)
- [`VertxDecoratorsTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/resilience4j/VertxDecoratorsTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Vert.x 모듈 기능 아키텍처 다이어그램

[![Vert.x 모듈 기능 아키텍처 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-01.svg)

_배포본 README: [`io/vertx/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/vertx/README.ko.md)_

### Vert.x 이벤트 루프와 Coroutines 처리 흐름 다이어그램

[![Vert.x 이벤트 루프와 Coroutines 처리 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-02.svg)

_배포본 README: [`io/vertx/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/vertx/README.ko.md)_

### Vert.x 핵심 컴포넌트 클래스 구조 다이어그램

[![Vert.x 핵심 컴포넌트 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-03.svg)

_배포본 README: [`io/vertx/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/vertx/README.ko.md)_

### Circuit Breaker와 Resilience4j 통합 흐름 시퀀스 다이어그램

[![Circuit Breaker와 Resilience4j 통합 흐름 시퀀스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-sequence-01.svg)

_배포본 README: [`io/vertx/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/vertx/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../io/vertx/README.ko.md)
- [모듈 build](../../../../io/vertx/build.gradle.kts)
- [`CoroutineSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/CoroutineSupport.kt)
- [`FutureExtensions`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/FutureExtensions.kt)
- [`VertxSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/VertxSupport.kt)
- [`VertxDecorators`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxDecorators.kt)
- [`VertxFutureBulkheadSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureBulkheadSupport.kt)
- [`VertxFutureCircuitBreakerSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureCircuitBreakerSupport.kt)
- [`VertxFutureRateLimiterSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureRateLimiterSupport.kt)
- [`VertxFutureRetrySupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureRetrySupport.kt)
- [`VertxFutureSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureSupport.kt)
- [`VertxFutureTimeLimiterSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureTimeLimiterSupport.kt)
- [`AbstractVertxTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/AbstractVertxTest.kt)
- [`SampleVerticle`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/SampleVerticle.kt)
