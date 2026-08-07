---
manualId: bluetape4k-ktor-testing
title: "Ktor 테스트 지원"
description: "bluetape4k 생태계를 위한 Ktor 테스트 helper 모듈입니다."
kind: library
group: web
learningOrder: 810
---

# Ktor 테스트 지원

## 해결하는 문제 {#problem}

bluetape4k 생태계를 위한 Ktor 테스트 helper 모듈입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 request lifecycle, cancellation, routing, context propagation, test boundary이 필요할 때 `bluetape4k-ktor-testing`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-ktor-testing")
}
```

Gradle project path는 `:bluetape4k-ktor-testing`, source directory는 `ktor/testing`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `Bluetape4kKtorTesting`, `ExpectedApiError`, `KtorJsonMockEngineSupport`, `KtorResponseAssertions`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`Bluetape4kKtorTesting`](../../../../ktor/testing/src/main/kotlin/io/bluetape4k/ktor/testing/Bluetape4kKtorTesting.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`Bluetape4kKtorTesting`](../../../../ktor/testing/src/main/kotlin/io/bluetape4k/ktor/testing/Bluetape4kKtorTesting.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ExpectedApiError`](../../../../ktor/testing/src/main/kotlin/io/bluetape4k/ktor/testing/ExpectedApiError.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`KtorJsonMockEngineSupport`](../../../../ktor/testing/src/main/kotlin/io/bluetape4k/ktor/testing/KtorJsonMockEngineSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`KtorResponseAssertions`](../../../../ktor/testing/src/main/kotlin/io/bluetape4k/ktor/testing/KtorResponseAssertions.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **Sequence Diagram**, **기능**, **의존성**, **사용 예** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-ktor-core"))
api(project(":bluetape4k-assertions"))
api(libs.ktor.server.test.host)
api(libs.ktor.client.core)
api(libs.ktor.client.content.negotiation)
api(libs.ktor.client.mock)
implementation(libs.ktor.serialization.kotlinx.json)
implementation(libs.kotlinx.serialization.json)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

request latency, status code, cancellation, queueing, dependency failure, shutdown을 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-ktor-testing:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`Bluetape4kKtorTestingTest`](../../../../ktor/testing/src/test/kotlin/io/bluetape4k/ktor/testing/Bluetape4kKtorTestingTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Ktor Testing 처리 순서

[![Ktor Testing 처리 순서](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/ktor-testing-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/ktor-testing-sequence-01.svg)

_배포본 README: [`ktor/testing/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/ktor/testing/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../ktor/testing/README.ko.md)
- [모듈 build](../../../../ktor/testing/build.gradle.kts)
- [`Bluetape4kKtorTesting`](../../../../ktor/testing/src/main/kotlin/io/bluetape4k/ktor/testing/Bluetape4kKtorTesting.kt)
- [`ExpectedApiError`](../../../../ktor/testing/src/main/kotlin/io/bluetape4k/ktor/testing/ExpectedApiError.kt)
- [`KtorJsonMockEngineSupport`](../../../../ktor/testing/src/main/kotlin/io/bluetape4k/ktor/testing/KtorJsonMockEngineSupport.kt)
- [`KtorResponseAssertions`](../../../../ktor/testing/src/main/kotlin/io/bluetape4k/ktor/testing/KtorResponseAssertions.kt)
- [`Bluetape4kKtorTestingTest`](../../../../ktor/testing/src/test/kotlin/io/bluetape4k/ktor/testing/Bluetape4kKtorTestingTest.kt)
