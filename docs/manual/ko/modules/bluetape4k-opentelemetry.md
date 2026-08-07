---
manualId: bluetape4k-opentelemetry
title: "OpenTelemetry 추적"
description: "OpenTelemetry는 클라우드 네이티브 소프트웨어를 위한 관측 가능성 프레임워크입니다. 이 모듈은 OpenTelemetry를 Kotlin에서 더욱 쉽고 편리하게 사용할 수 있도록 하는 확장 함수와 유틸리티를 제공합니다."
kind: library
group: operations
learningOrder: 1030
---

# OpenTelemetry 추적

## 해결하는 문제 {#problem}

OpenTelemetry는 클라우드 네이티브 소프트웨어를 위한 관측 가능성 프레임워크입니다. 이 모듈은 OpenTelemetry를 Kotlin에서 더욱 쉽고 편리하게 사용할 수 있도록 하는 확장 함수와 유틸리티를 제공합니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 client lifecycle, reconnect policy, backpressure, retry, observability이 필요할 때 `bluetape4k-opentelemetry`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-opentelemetry")
}
```

Gradle project path는 `:bluetape4k-opentelemetry`, source directory는 `infra/opentelemetry`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `ContextExtensions`, `OpenTelemetrySupport`, `AttributeKeySupport`, `AttributesSupport`, `CompletableResultCodeSupport`, `ContextCoroutineSupport`, `FlowSpanSupport`, `SpanCoroutineSupport`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`ContextExtensions`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/ContextExtensions.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`ContextExtensions`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/ContextExtensions.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`OpenTelemetrySupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/OpenTelemetrySupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`AttributeKeySupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/common/AttributeKeySupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`AttributesSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/common/AttributesSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CompletableResultCodeSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/CompletableResultCodeSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ContextCoroutineSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/ContextCoroutineSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`FlowSpanSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/FlowSpanSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SpanCoroutineSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/SpanCoroutineSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MeterProviderSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/metrics/MeterProviderSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MetricExporterSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/metrics/MetricExporterSupport.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **특징**, **아키텍처 다이어그램**, **OpenTelemetry 핵심 클래스 구조**, **OpenTelemetry 구성 요소**, **Span 생명주기 (Coroutines 환경)**, **분산 추적 전파 흐름**, **의존성**, **주요 기능**, **1. OpenTelemetry SDK 설정**, **2. Tracer 생성 및 Span 관리** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-io"))
implementation(project(":bluetape4k-netty"))
api(libs.opentelemetry.api)
api(libs.opentelemetry.sdk)
api(libs.opentelemetry.extension.kotlin)
compileOnly(libs.opentelemetry.sdk.extensions.autoconfigure)
compileOnly(libs.opentelemetry.sdk.metrics)
compileOnly(libs.opentelemetry.sdk.logs)
compileOnly(libs.opentelemetry.sdk.trace)
compileOnly(libs.opentelemetry.sdk.testing)
compileOnly(libs.opentelemetry.exporter.logging)
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
./gradlew :bluetape4k-opentelemetry:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractOtelTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/AbstractOtelTest.kt)
- [`RedactionAssertions`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/RedactionAssertions.kt)
- [`AttributeKeySupportTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/common/AttributeKeySupportTest.kt)
- [`AttributesSupportTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/common/AttributesSupportTest.kt)
- [`CoroutineSupportTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/coroutines/CoroutineSupportTest.kt)
- [`FlowSpanSupportTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/coroutines/FlowSpanSupportTest.kt)
- [`SpanCoroutineSupportTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/coroutines/SpanCoroutineSupportTest.kt)
- [`TracerWithSpanTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/coroutines/TracerWithSpanTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### OpenTelemetry 핵심 클래스 구조 다이어그램

[![OpenTelemetry 핵심 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-01.svg)

_배포본 README: [`infra/opentelemetry/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/opentelemetry/README.ko.md)_

### OpenTelemetry 구성 요소 다이어그램

[![OpenTelemetry 구성 요소 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-02.svg)

_배포본 README: [`infra/opentelemetry/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/opentelemetry/README.ko.md)_

### 분산 추적 전파 다이어그램

[![분산 추적 전파 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-03.svg)

_배포본 README: [`infra/opentelemetry/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/opentelemetry/README.ko.md)_

### Coroutine 환경의 Span 생명주기 시퀀스 다이어그램

[![Coroutine 환경의 Span 생명주기 시퀀스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-sequence-01.svg)

_배포본 README: [`infra/opentelemetry/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/opentelemetry/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../infra/opentelemetry/README.ko.md)
- [모듈 build](../../../../infra/opentelemetry/build.gradle.kts)
- [`ContextExtensions`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/ContextExtensions.kt)
- [`OpenTelemetrySupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/OpenTelemetrySupport.kt)
- [`AttributeKeySupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/common/AttributeKeySupport.kt)
- [`AttributesSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/common/AttributesSupport.kt)
- [`CompletableResultCodeSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/CompletableResultCodeSupport.kt)
- [`ContextCoroutineSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/ContextCoroutineSupport.kt)
- [`FlowSpanSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/FlowSpanSupport.kt)
- [`SpanCoroutineSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/SpanCoroutineSupport.kt)
- [`MeterProviderSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/metrics/MeterProviderSupport.kt)
- [`MetricExporterSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/metrics/MetricExporterSupport.kt)
- [`AbstractOtelTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/AbstractOtelTest.kt)
- [`RedactionAssertions`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/RedactionAssertions.kt)
