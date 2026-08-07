---
manualId: bluetape4k-mock-web-server
title: "HTTP 모의 서버"
description: "외부 HTTP 의존성을 통합 테스트에서 대체하기 위한 독립형 Spring Boot 4 + Virtual Threads HTTP Mock 서버입니다. httpbin.org, jsonplaceholder.typicode.com, 간단한 웹 콘텐츠 엔드포인트를 하나의 Docker 이미지 (bluetape4k/mock-web-server)로 제공합니다."
kind: library
group: testing
learningOrder: 1130
---

# HTTP 모의 서버

## 해결하는 문제 {#problem}

외부 HTTP 의존성을 통합 테스트에서 대체하기 위한 독립형 Spring Boot 4 + Virtual Threads HTTP Mock 서버입니다. httpbin.org, jsonplaceholder.typicode.com, 간단한 웹 콘텐츠 엔드포인트를 하나의 Docker 이미지 (bluetape4k/mock-web-server)로 제공합니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 fixture ownership, isolation, deterministic cleanup, failure diagnostic이 필요할 때 `bluetape4k-mock-web-server`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-mock-web-server")
}
```

Gradle project path는 `:bluetape4k-mock-web-server`, source directory는 `testing/mock-web-server`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `MockServerApplication`, `AdminController`, `PingController`, `GlobalExceptionHandler`, `HttpsConfiguration`, `HttpbinAdvancedController`, `HttpbinController`, `HttpbinStreamController`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`MockServerApplication`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/MockServerApplication.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`MockServerApplication`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/MockServerApplication.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`AdminController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/admin/AdminController.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`PingController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/admin/PingController.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`GlobalExceptionHandler`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/config/GlobalExceptionHandler.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpsConfiguration`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/config/HttpsConfiguration.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpbinAdvancedController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinAdvancedController.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpbinController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinController.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpbinStreamController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinStreamController.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HttpbinSupport`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ImageLoaderService`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/ImageLoaderService.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **아키텍처**, **다이어그램**, **요청 라우팅 개요**, **클래스 다이어그램**, **시퀀스 다이어그램 — httpbin GET 요청**, **기능**, **설정**, **예제**, **Docker로 실행**, **Jib으로 Docker 이미지 빌드** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
implementation(platform(libs.jackson3.bom))
implementation("org.springframework.boot:spring-boot-starter-web")
implementation("org.springframework.boot:spring-boot-starter-cache")
implementation(libs.caffeine)
implementation(libs.jackson3.module.kotlin)
implementation(project(":bluetape4k-core"))
implementation(project(":bluetape4k-logging"))
implementation(project(":bluetape4k-jackson3"))
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

모듈에서 찾은 설정 resource는 다음과 같습니다.

- [`application.yml`](../../../../testing/mock-web-server/src/main/resources/application.yml)
- [`localhost.p12`](../../../../testing/mock-web-server/src/main/resources/certs/localhost.p12)
- [`rootCA.pem`](../../../../testing/mock-web-server/src/main/resources/certs/rootCA.pem)
- [`albums.json`](../../../../testing/mock-web-server/src/main/resources/jsonplaceholder/albums.json)
- [`comments.json`](../../../../testing/mock-web-server/src/main/resources/jsonplaceholder/comments.json)
- [`photos.json`](../../../../testing/mock-web-server/src/main/resources/jsonplaceholder/photos.json)
- [`posts.json`](../../../../testing/mock-web-server/src/main/resources/jsonplaceholder/posts.json)
- [`todos.json`](../../../../testing/mock-web-server/src/main/resources/jsonplaceholder/todos.json)

override하기 전에 이 resource와 binding source에서 property 이름과 default를 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

fixture를 격리하고 resource 사용량을 제한하며 diagnostic을 남기고 shared service를 확실히 닫습니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-mock-web-server:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`MockServerTestBase`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/MockServerTestBase.kt)
- [`ReadmeHttpsPortContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/ReadmeHttpsPortContractTest.kt)
- [`AdminResetContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/admin/AdminResetContractTest.kt)
- [`PingContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/admin/PingContractTest.kt)
- [`HttpbinAdvancedContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinAdvancedContractTest.kt)
- [`HttpbinContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinContractTest.kt)
- [`HttpbinStreamContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinStreamContractTest.kt)
- [`HttpbinSupportTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinSupportTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Mock Web Server 요청 라우팅 개요

[![Mock Web Server 요청 라우팅 개요](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-diagram-01.svg)

_배포본 README: [`testing/mock-web-server/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-web-server/README.ko.md)_

### Mock Web Server 클래스 구조

[![Mock Web Server 클래스 구조](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-diagram-02.svg)

_배포본 README: [`testing/mock-web-server/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-web-server/README.ko.md)_

### httpbin GET 요청 시퀀스

[![httpbin GET 요청 시퀀스](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-sequence-01.svg)

_배포본 README: [`testing/mock-web-server/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-web-server/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../testing/mock-web-server/README.ko.md)
- [모듈 build](../../../../testing/mock-web-server/build.gradle.kts)
- [`MockServerApplication`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/MockServerApplication.kt)
- [`AdminController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/admin/AdminController.kt)
- [`PingController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/admin/PingController.kt)
- [`GlobalExceptionHandler`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/config/GlobalExceptionHandler.kt)
- [`HttpsConfiguration`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/config/HttpsConfiguration.kt)
- [`HttpbinAdvancedController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinAdvancedController.kt)
- [`HttpbinController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinController.kt)
- [`HttpbinStreamController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinStreamController.kt)
- [`HttpbinSupport`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinSupport.kt)
- [`ImageLoaderService`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/ImageLoaderService.kt)
- [`MockServerTestBase`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/MockServerTestBase.kt)
- [`ReadmeHttpsPortContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/ReadmeHttpsPortContractTest.kt)
