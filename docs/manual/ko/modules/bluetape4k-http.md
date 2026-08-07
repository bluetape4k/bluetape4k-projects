---
manualId: bluetape4k-http
title: "HTTP 클라이언트 기반 기능"
description: "bluetape4k-http는 다양한 HTTP 클라이언트 라이브러리를 Kotlin 확장 함수와 DSL로 통합하여 제공하는 모듈입니다."
kind: library
group: io
learningOrder: 400
---

# HTTP 클라이언트 기반 기능

## 해결하는 문제 {#problem}

bluetape4k-http는 다양한 HTTP 클라이언트 라이브러리를 Kotlin 확장 함수와 DSL로 통합하여 제공하는 모듈입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 encoding boundary, resource ownership, streaming, 호환성, malformed input이 필요할 때 `bluetape4k-http`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-http")
}
```

Gradle project path는 `:bluetape4k-http`, source directory는 `io/http`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `AsyncClientConnectionManager`, `CloseableHttpAsyncClientCoroutines`, `HttpAsyncClient`, `HttpAsyncClientCoroutines`, `MinimalHttpAsyncClient`, `ConfigurableHttpRequest`, `SimpleHttpRequest`, `SimpleHttpResponse`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`AsyncClientConnectionManager`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/AsyncClientConnectionManager.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`AsyncClientConnectionManager`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/AsyncClientConnectionManager.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CloseableHttpAsyncClientCoroutines`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/CloseableHttpAsyncClientCoroutines.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpAsyncClient`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/HttpAsyncClient.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpAsyncClientCoroutines`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/HttpAsyncClientCoroutines.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MinimalHttpAsyncClient`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/MinimalHttpAsyncClient.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ConfigurableHttpRequest`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/ConfigurableHttpRequest.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SimpleHttpRequest`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleHttpRequest.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SimpleHttpResponse`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleHttpResponse.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SimpleRequestProducer`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleRequestProducer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SimpleResponseConsumer`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleResponseConsumer.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **개요**, **아키텍처**, **전체 아키텍처: 다중 백엔드 HTTP 클라이언트**, **HTTP 클라이언트 계층 (HC5)**, **OkHttp3 클라이언트 계층**, **비동기 HTTP 요청 흐름 (HC5 Async + Coroutines)**, **주요 기능**, **1. Apache HttpComponents 5 (HC5)**, **2. OkHttp3**, **3. Vert.x HttpClient** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-io"))
api(project(":bluetape4k-netty"))
api(project(":bluetape4k-resilience4j"))
api(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
compileOnly(libs.kotlinx.coroutines.reactive)
compileOnly(libs.kotlinx.coroutines.reactor)
compileOnly(libs.okhttp3)
compileOnly(libs.okhttp3.coroutines)
compileOnly(libs.okhttp3.logging.interceptor)
compileOnly(libs.okhttp3.mockwebserver)
compileOnly(libs.httpclient5)
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
./gradlew :bluetape4k-http:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractHttpTest`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/AbstractHttpTest.kt)
- [`HttpClientBenchmark`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/benchmark/HttpClientBenchmark.kt)
- [`HttpClientBenchmarkTest`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/benchmark/HttpClientBenchmarkTest.kt)
- [`HttpClientCompressionCacheBenchmark`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/benchmark/HttpClientCompressionCacheBenchmark.kt)
- [`HttpClientLatencyBenchmark`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/benchmark/HttpClientLatencyBenchmark.kt)
- [`AbstractHc5Test`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/hc5/AbstractHc5Test.kt)
- [`AsyncHttpClientCoroutinesTest`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/hc5/async/AsyncHttpClientCoroutinesTest.kt)
- [`AsyncHttpClientTest`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/hc5/async/AsyncHttpClientTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### HTTP client base throughput 차트

[![HTTP client base throughput 차트](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-chart-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-chart-01.svg)

_배포본 README: [`io/http/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/http/README.ko.md)_

### HTTP client high-latency benchmark 차트

[![HTTP client high-latency benchmark 차트](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-chart-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-chart-02.svg)

_배포본 README: [`io/http/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/http/README.ko.md)_

### 다중 백엔드 HTTP 클라이언트 아키텍처 다이어그램

[![다중 백엔드 HTTP 클라이언트 아키텍처 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-01.svg)

_배포본 README: [`io/http/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/http/README.ko.md)_

### HC5 HTTP 클라이언트 계층 다이어그램

[![HC5 HTTP 클라이언트 계층 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-02.svg)

_배포본 README: [`io/http/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/http/README.ko.md)_

### OkHttp3 클라이언트 계층 다이어그램

[![OkHttp3 클라이언트 계층 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-03.svg)

_배포본 README: [`io/http/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/http/README.ko.md)_

### HTTP 클라이언트 주요 권장 선택 다이어그램

[![HTTP 클라이언트 주요 권장 선택 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-04.svg)

_배포본 README: [`io/http/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/http/README.ko.md)_

### Profiling workflow

[![Profiling workflow](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-05.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-05.svg)

_배포본 README: [`io/http/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/http/README.ko.md)_

### Profiling mode comparison

[![Profiling mode comparison](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-06.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-diagram-06.svg)

_배포본 README: [`io/http/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/http/README.ko.md)_

### HC5 Async와 Coroutines 기반 비동기 HTTP 요청 시퀀스 다이어그램

[![HC5 Async와 Coroutines 기반 비동기 HTTP 요청 시퀀스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-http-sequence-01.svg)

_배포본 README: [`io/http/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/http/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../io/http/README.ko.md)
- [모듈 build](../../../../io/http/build.gradle.kts)
- [`AsyncClientConnectionManager`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/AsyncClientConnectionManager.kt)
- [`CloseableHttpAsyncClientCoroutines`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/CloseableHttpAsyncClientCoroutines.kt)
- [`HttpAsyncClient`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/HttpAsyncClient.kt)
- [`HttpAsyncClientCoroutines`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/HttpAsyncClientCoroutines.kt)
- [`MinimalHttpAsyncClient`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/MinimalHttpAsyncClient.kt)
- [`ConfigurableHttpRequest`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/ConfigurableHttpRequest.kt)
- [`SimpleHttpRequest`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleHttpRequest.kt)
- [`SimpleHttpResponse`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleHttpResponse.kt)
- [`SimpleRequestProducer`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleRequestProducer.kt)
- [`SimpleResponseConsumer`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleResponseConsumer.kt)
- [`AbstractHttpTest`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/AbstractHttpTest.kt)
- [`HttpClientBenchmark`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/benchmark/HttpClientBenchmark.kt)
