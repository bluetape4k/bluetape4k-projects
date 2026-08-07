---
manualId: bluetape4k-resilience4j
title: "Resilience4j 회복 탄력성 패턴"
description: "Resilience4j는 장애 격리와 회복성을 위한 경량 오픈소스 라이브러리입니다."
kind: library
group: operations
learningOrder: 1000
---

# Resilience4j 회복 탄력성 패턴

## 해결하는 문제 {#problem}

Resilience4j는 장애 격리와 회복성을 위한 경량 오픈소스 라이브러리입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 client lifecycle, reconnect policy, backpressure, retry, observability이 필요할 때 `bluetape4k-resilience4j`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-resilience4j")
}
```

Gradle project path는 `:bluetape4k-resilience4j`, source directory는 `infra/resilience4j`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `RetryAsyncContextBridge`, `CallableSupport`, `CancellationSupport`, `CompletionStageSupport`, `DecoratorsSupport`, `SupplierSupport`, `SuspendDecorators`, `SuspendSupplierExtensions`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`RetryAsyncContextBridge`](../../../../infra/resilience4j/src/main/java/io/bluetape4k/resilience4j/retry/RetryAsyncContextBridge.java)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`RetryAsyncContextBridge`](../../../../infra/resilience4j/src/main/java/io/bluetape4k/resilience4j/retry/RetryAsyncContextBridge.java) | constructor, function, ownership 계약을 확인합니다. |
| [`CallableSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CallableSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CancellationSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CancellationSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CompletionStageSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CompletionStageSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`DecoratorsSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/DecoratorsSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SupplierSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SupplierSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SuspendDecorators`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendDecorators.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SuspendSupplierExtensions`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendSupplierExtensions.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`BulkheadCoroutines`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/bulkhead/BulkheadCoroutines.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`BulkheadExtensions`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/bulkhead/BulkheadExtensions.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **클래스 구조**, **Resilience4j Coroutine 클래스 구조**, **아키텍처**, **CircuitBreaker + Retry 조합 시퀀스 다이어그램**, **SuspendCache 동작 시퀀스 다이어그램**, **특징**, **모듈 경계**, **코루틴 계약**, **의존성**, **주요 기능** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-core"))
compileOnly(project(":bluetape4k-coroutines"))
compileOnly(project(":bluetape4k-cache-redisson"))
api(libs.resilience4j.all)
api(libs.resilience4j.cache)
api(libs.resilience4j.kotlin)
compileOnly(libs.resilience4j.reactor)
compileOnly(libs.resilience4j.micrometer)
compileOnly(libs.kotlinx.coroutines.core)
compileOnly(libs.kotlinx.coroutines.reactor)
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
./gradlew :bluetape4k-resilience4j:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AsyncHelloWorldService`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/AsyncHelloWorldService.kt)
- [`CallableSupportTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/CallableSupportTest.kt)
- [`CompletionStageSupportTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/CompletionStageSupportTest.kt)
- [`DecoratorExtensionsTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/DecoratorExtensionsTest.kt)
- [`DecoratorsTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/DecoratorsTest.kt)
- [`HelloWorldException`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/HelloWorldException.kt)
- [`HelloWorldService`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/HelloWorldService.kt)
- [`Resilience4jCancellationContractTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/Resilience4jCancellationContractTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Resilience4j Coroutine 클래스 구조 다이어그램

[![Resilience4j Coroutine 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-diagram-01.svg)

_배포본 README: [`infra/resilience4j/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/resilience4j/README.ko.md)_

### CircuitBreaker + Retry 다이어그램

[![CircuitBreaker + Retry 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-sequence-01.svg)

_배포본 README: [`infra/resilience4j/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/resilience4j/README.ko.md)_

### SuspendCache 다이어그램

[![SuspendCache 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-sequence-02.svg)

_배포본 README: [`infra/resilience4j/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/resilience4j/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../infra/resilience4j/README.ko.md)
- [모듈 build](../../../../infra/resilience4j/build.gradle.kts)
- [`RetryAsyncContextBridge`](../../../../infra/resilience4j/src/main/java/io/bluetape4k/resilience4j/retry/RetryAsyncContextBridge.java)
- [`CallableSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CallableSupport.kt)
- [`CancellationSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CancellationSupport.kt)
- [`CompletionStageSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CompletionStageSupport.kt)
- [`DecoratorsSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/DecoratorsSupport.kt)
- [`SupplierSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SupplierSupport.kt)
- [`SuspendDecorators`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendDecorators.kt)
- [`SuspendSupplierExtensions`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendSupplierExtensions.kt)
- [`BulkheadCoroutines`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/bulkhead/BulkheadCoroutines.kt)
- [`BulkheadExtensions`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/bulkhead/BulkheadExtensions.kt)
- [`AsyncHelloWorldService`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/AsyncHelloWorldService.kt)
- [`CallableSupportTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/CallableSupportTest.kt)
