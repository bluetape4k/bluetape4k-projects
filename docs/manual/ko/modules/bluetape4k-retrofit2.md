---
manualId: bluetape4k-retrofit2
title: "Retrofit 2 코루틴 확장"
description: "bluetape4k-retrofit2는 Retrofit2를 Kotlin DSL과 Coroutines로 확장하여 제공하는 모듈입니다."
kind: library
group: io
learningOrder: 420
---

# Retrofit 2 코루틴 확장

## 해결하는 문제 {#problem}

bluetape4k-retrofit2는 Retrofit2를 Kotlin DSL과 Coroutines로 확장하여 제공하는 모듈입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 encoding boundary, resource ownership, streaming, 호환성, malformed input이 필요할 때 `bluetape4k-retrofit2`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-retrofit2")
}
```

Gradle project path는 `:bluetape4k-retrofit2`, source directory는 `io/retrofit2`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `ExceptionSupport`, `RetrofitCallSupport`, `RetrofitSupport`, `SuspendRetrofitCallSupport`, `Hc5CallFactory`, `Hc5OkHttp3Support`, `VertxCallFactory`, `VertxOkHttp3Support`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`ExceptionSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/ExceptionSupport.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`ExceptionSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/ExceptionSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RetrofitCallSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/RetrofitCallSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RetrofitSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/RetrofitSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SuspendRetrofitCallSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/SuspendRetrofitCallSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`Hc5CallFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/hc5/Hc5CallFactory.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`Hc5OkHttp3Support`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/hc5/Hc5OkHttp3Support.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VertxCallFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/vertx/VertxCallFactory.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VertxOkHttp3Support`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/vertx/VertxOkHttp3Support.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ResultCall`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/result/ResultCall.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ResultCallAdapterFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/result/ResultCallAdapterFactory.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **개요**, **아키텍처**, **Retrofit2 모듈 아키텍처**, **Retrofit2 + Result 패턴 통합 구조**, **suspend Result HTTP 요청 흐름**, **주요 기능**, **1. Retrofit Builder DSL**, **2. Result 패턴 지원**, **3. Coroutines 지원**, **4. 다양한 HTTP 전송 계층 (CallFactory)** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-http"))
api(project(":bluetape4k-okio"))
api(project(":bluetape4k-netty"))
api(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
compileOnly(libs.kotlinx.coroutines.reactive)
compileOnly(libs.kotlinx.coroutines.reactor)
api(libs.retrofit2)
api(libs.retrofit2.converter.jackson)
api(libs.retrofit2.converter.scalars)
api(libs.retrofit2.adapter.java8)
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
./gradlew :bluetape4k-retrofit2:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractRetrofitTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/AbstractRetrofitTest.kt)
- [`ExceptionSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/ExceptionSupportTest.kt)
- [`RetrofitBuilderSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/RetrofitBuilderSupportTest.kt)
- [`RetrofitSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/RetrofitSupportTest.kt)
- [`RetryRetrofitCallSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/RetryRetrofitCallSupportTest.kt)
- [`SuspendRetrofitCallSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/SuspendRetrofitCallSupportTest.kt)
- [`AbstractClientTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/client/AbstractClientTest.kt)
- [`AbstractDetectTempEmailTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/client/AbstractDetectTempEmailTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Retrofit2 모듈 아키텍처 다이어그램

[![Retrofit2 모듈 아키텍처 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-diagram-01.svg)

_배포본 README: [`io/retrofit2/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/retrofit2/README.ko.md)_

### Retrofit2 Result 패턴 통합 구조 다이어그램

[![Retrofit2 Result 패턴 통합 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-diagram-02.svg)

_배포본 README: [`io/retrofit2/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/retrofit2/README.ko.md)_

### suspend Result HTTP 요청 흐름 시퀀스 다이어그램

[![suspend Result HTTP 요청 흐름 시퀀스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-sequence-01.svg)

_배포본 README: [`io/retrofit2/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/retrofit2/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../io/retrofit2/README.ko.md)
- [모듈 build](../../../../io/retrofit2/build.gradle.kts)
- [`ExceptionSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/ExceptionSupport.kt)
- [`RetrofitCallSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/RetrofitCallSupport.kt)
- [`RetrofitSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/RetrofitSupport.kt)
- [`SuspendRetrofitCallSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/SuspendRetrofitCallSupport.kt)
- [`Hc5CallFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/hc5/Hc5CallFactory.kt)
- [`Hc5OkHttp3Support`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/hc5/Hc5OkHttp3Support.kt)
- [`VertxCallFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/vertx/VertxCallFactory.kt)
- [`VertxOkHttp3Support`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/vertx/VertxOkHttp3Support.kt)
- [`ResultCall`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/result/ResultCall.kt)
- [`ResultCallAdapterFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/result/ResultCallAdapterFactory.kt)
- [`AbstractRetrofitTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/AbstractRetrofitTest.kt)
- [`ExceptionSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/ExceptionSupportTest.kt)
