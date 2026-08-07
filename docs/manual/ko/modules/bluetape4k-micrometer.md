---
manualId: bluetape4k-micrometer
title: "Micrometer 메트릭 확장"
description: "Micrometer와 Observation API를 활용한 애플리케이션 성능 측정 및 관찰(observability) 기능을 제공하는 모듈입니다."
kind: library
group: operations
learningOrder: 1020
---

# Micrometer 메트릭 확장

## 해결하는 문제 {#problem}

Micrometer와 Observation API를 활용한 애플리케이션 성능 측정 및 관찰(observability) 기능을 제공하는 모듈입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 client lifecycle, reconnect policy, backpressure, retry, observability이 필요할 때 `bluetape4k-micrometer`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-micrometer")
}
```

Gradle project path는 `:bluetape4k-micrometer`, source directory는 `infra/micrometer`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `KeyValueSupport`, `TimerExtensions`, `Cache2kCacheMetrics`, `MeasuredCall`, `MeasuredCallAdapter`, `MetricsRecorder`, `MicrometerRetrofitMetricsFactory`, `MicrometerRetrofitMetricsRecorder`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`KeyValueSupport`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/common/KeyValueSupport.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`KeyValueSupport`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/common/KeyValueSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`TimerExtensions`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/TimerExtensions.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`Cache2kCacheMetrics`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/cache/Cache2kCacheMetrics.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MeasuredCall`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MeasuredCall.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MeasuredCallAdapter`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MeasuredCallAdapter.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MetricsRecorder`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MetricsRecorder.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MicrometerRetrofitMetricsFactory`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MicrometerRetrofitMetricsFactory.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MicrometerRetrofitMetricsRecorder`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MicrometerRetrofitMetricsRecorder.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`Outcome`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/Outcome.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RetrofitCallMetricsCollector`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/RetrofitCallMetricsCollector.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **개요**, **아키텍처**, **핵심 클래스 구조**, **메트릭 수집 흐름**, **Retrofit2 메트릭 수집 시퀀스**, **Coroutine Observation 흐름**, **의존성**, **주요 기능**, **1. Timer 확장 (TimerExtensions)**, **Suspend 함수 실행 시간 측정** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
implementation(project(":bluetape4k-cache-core"))
api(libs.micrometer.core)
implementation(libs.micrometer.registry.prometheus)
implementation(libs.micrometer.registry.datadog)
api(libs.micrometer.observation)
implementation(libs.micrometer.observation.test)
implementation(libs.micrometer.tracing.bridge.otel)
api(libs.micrometer.context.propagation)  // thread local <-> reactor 등 상이한 환경에서 context 전파를 위해 사용
implementation(libs.cache2k.core)
implementation(project(":bluetape4k-retrofit2"))
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
./gradlew :bluetape4k-micrometer:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`KeyValueSupportTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/common/KeyValueSupportTest.kt)
- [`AbstractMicrometerTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/AbstractMicrometerTest.kt)
- [`TimerExtensionsTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/TimerExtensionsTest.kt)
- [`Cache2kCacheMetricsTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/cache/Cache2kCacheMetricsTest.kt)
- [`OutcomeTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/OutcomeTest.kt)
- [`Retrofit2MetricsTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/Retrofit2MetricsTest.kt)
- [`RetrofitMetricsSupportTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/RetrofitMetricsSupportTest.kt)
- [`RetrofitMetricsUnitTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/RetrofitMetricsUnitTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### 핵심 클래스 구조 다이어그램

[![핵심 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-01.svg)

_배포본 README: [`infra/micrometer/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.ko.md)_

### 메트릭 수집 흐름 다이어그램

[![메트릭 수집 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-02.svg)

_배포본 README: [`infra/micrometer/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.ko.md)_

### Micrometer 모듈 아키텍처 다이어그램

[![Micrometer 모듈 아키텍처 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-03.svg)

_배포본 README: [`infra/micrometer/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.ko.md)_

### Retrofit2 메트릭 수집 시퀀스 다이어그램

[![Retrofit2 메트릭 수집 시퀀스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-01.svg)

_배포본 README: [`infra/micrometer/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.ko.md)_

### Coroutine Observation 흐름 다이어그램

[![Coroutine Observation 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-02.svg)

_배포본 README: [`infra/micrometer/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.ko.md)_

### Event Telemetry 시퀀스 다이어그램

[![Event Telemetry 시퀀스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-03.svg)

_배포본 README: [`infra/micrometer/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../infra/micrometer/README.ko.md)
- [모듈 build](../../../../infra/micrometer/build.gradle.kts)
- [`KeyValueSupport`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/common/KeyValueSupport.kt)
- [`TimerExtensions`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/TimerExtensions.kt)
- [`Cache2kCacheMetrics`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/cache/Cache2kCacheMetrics.kt)
- [`MeasuredCall`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MeasuredCall.kt)
- [`MeasuredCallAdapter`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MeasuredCallAdapter.kt)
- [`MetricsRecorder`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MetricsRecorder.kt)
- [`MicrometerRetrofitMetricsFactory`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MicrometerRetrofitMetricsFactory.kt)
- [`MicrometerRetrofitMetricsRecorder`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MicrometerRetrofitMetricsRecorder.kt)
- [`Outcome`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/Outcome.kt)
- [`RetrofitCallMetricsCollector`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/RetrofitCallMetricsCollector.kt)
- [`KeyValueSupportTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/common/KeyValueSupportTest.kt)
- [`AbstractMicrometerTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/AbstractMicrometerTest.kt)
