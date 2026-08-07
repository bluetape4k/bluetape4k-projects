---
manualId: bluetape4k-mock-webflux-server
title: "WebFlux 모의 서버"
description: "통합 테스트용 독립 실행형 Spring Boot 4 + WebFlux Mock 서버입니다. httpbin.org, jsonplaceholder.typicode.com와 호환되는 HTTP 엔드포인트를 Kotlin Coroutines (suspend fun, Flow)로 구현합니다. Docker 컨테이너 내부에서 80(HTTP) / 8443(HTTPS) 포트로 실행됩니다."
kind: library
group: testing
learningOrder: 1140
---

# WebFlux 모의 서버

## 해결하는 문제 {#problem}

통합 테스트용 독립 실행형 Spring Boot 4 + WebFlux Mock 서버입니다. httpbin.org, jsonplaceholder.typicode.com와 호환되는 HTTP 엔드포인트를 Kotlin Coroutines (suspend fun, Flow)로 구현합니다. Docker 컨테이너 내부에서 80(HTTP) / 8443(HTTPS) 포트로 실행됩니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 fixture ownership, isolation, deterministic cleanup, failure diagnostic이 필요할 때 `bluetape4k-mock-webflux-server`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-mock-webflux-server")
}
```

Gradle project path는 `:bluetape4k-mock-webflux-server`, source directory는 `testing/mock-webflux-server`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `MockWebfluxServerApplication`, `AdminController`, `PingController`, `GlobalExceptionHandler`, `HttpsServerLifecycle`, `WebFluxJacksonConfig`, `HttpbinAdvancedController`, `HttpbinController`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`MockWebfluxServerApplication`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/MockWebfluxServerApplication.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`MockWebfluxServerApplication`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/MockWebfluxServerApplication.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`AdminController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/admin/AdminController.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`PingController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/admin/PingController.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`GlobalExceptionHandler`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/GlobalExceptionHandler.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpsServerLifecycle`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/HttpsServerLifecycle.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`WebFluxJacksonConfig`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/WebFluxJacksonConfig.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpbinAdvancedController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinAdvancedController.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpbinController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinController.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpbinStreamController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinStreamController.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpbinSupport`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinSupport.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **아키텍처**, **bluetape4k-mock-web-server와 비교**, **다이어그램**, **요청 라우팅 개요**, **클래스 다이어그램**, **시퀀스 다이어그램 — httpbin GET 요청**, **기능**, **설정**, **예제**, **Docker로 실행** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
implementation(platform(libs.jackson3.bom))
implementation("org.springframework.boot:spring-boot-starter-webflux")
implementation("org.springframework.boot:spring-boot-starter-cache")
implementation("org.springframework.boot:spring-boot-starter-actuator")
implementation(libs.caffeine)
implementation(libs.jackson3.module.kotlin)
implementation(libs.kotlinx.coroutines.core)
implementation(libs.kotlinx.coroutines.reactor)
implementation(project(":bluetape4k-core"))
implementation(project(":bluetape4k-coroutines"))
implementation(project(":bluetape4k-logging"))
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

모듈에서 찾은 설정 resource는 다음과 같습니다.

- [`application.yml`](../../../../testing/mock-webflux-server/src/main/resources/application.yml)
- [`localhost.p12`](../../../../testing/mock-webflux-server/src/main/resources/certs/localhost.p12)
- [`rootCA.pem`](../../../../testing/mock-webflux-server/src/main/resources/certs/rootCA.pem)
- [`albums.json`](../../../../testing/mock-webflux-server/src/main/resources/jsonplaceholder/albums.json)
- [`comments.json`](../../../../testing/mock-webflux-server/src/main/resources/jsonplaceholder/comments.json)
- [`photos.json`](../../../../testing/mock-webflux-server/src/main/resources/jsonplaceholder/photos.json)
- [`posts.json`](../../../../testing/mock-webflux-server/src/main/resources/jsonplaceholder/posts.json)
- [`todos.json`](../../../../testing/mock-webflux-server/src/main/resources/jsonplaceholder/todos.json)

override하기 전에 이 resource와 binding source에서 property 이름과 default를 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

fixture를 격리하고 resource 사용량을 제한하며 diagnostic을 남기고 shared service를 확실히 닫습니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-mock-webflux-server:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractMockWebfluxServerTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/AbstractMockWebfluxServerTest.kt)
- [`ReadmeRouteContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/ReadmeRouteContractTest.kt)
- [`AdminResetContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/admin/AdminResetContractTest.kt)
- [`PingContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/admin/PingContractTest.kt)
- [`GlobalExceptionHandlerTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/config/GlobalExceptionHandlerTest.kt)
- [`HttpbinAdvancedContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinAdvancedContractTest.kt)
- [`HttpbinContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinContractTest.kt)
- [`HttpbinStreamContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinStreamContractTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Mock WebFlux Server 요청 라우팅 개요

[![Mock WebFlux Server 요청 라우팅 개요](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-diagram-01.svg)

_배포본 README: [`testing/mock-webflux-server/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-webflux-server/README.ko.md)_

### Mock WebFlux Server 클래스 구조

[![Mock WebFlux Server 클래스 구조](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-diagram-02.svg)

_배포본 README: [`testing/mock-webflux-server/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-webflux-server/README.ko.md)_

### WebFlux httpbin GET 요청 시퀀스

[![WebFlux httpbin GET 요청 시퀀스](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-sequence-01.svg)

_배포본 README: [`testing/mock-webflux-server/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-webflux-server/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../testing/mock-webflux-server/README.ko.md)
- [모듈 build](../../../../testing/mock-webflux-server/build.gradle.kts)
- [`MockWebfluxServerApplication`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/MockWebfluxServerApplication.kt)
- [`AdminController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/admin/AdminController.kt)
- [`PingController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/admin/PingController.kt)
- [`GlobalExceptionHandler`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/GlobalExceptionHandler.kt)
- [`HttpsServerLifecycle`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/HttpsServerLifecycle.kt)
- [`WebFluxJacksonConfig`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/WebFluxJacksonConfig.kt)
- [`HttpbinAdvancedController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinAdvancedController.kt)
- [`HttpbinController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinController.kt)
- [`HttpbinStreamController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinStreamController.kt)
- [`HttpbinSupport`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinSupport.kt)
- [`AbstractMockWebfluxServerTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/AbstractMockWebfluxServerTest.kt)
- [`ReadmeRouteContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/ReadmeRouteContractTest.kt)
