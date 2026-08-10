---
manualId: bluetape4k-junit5
title: "JUnit 5 테스트 지원"
description: "JUnit 5 테스트 작성 시 반복 코드를 줄여주는 확장 라이브러리입니다."
kind: library
group: testing
learningOrder: 1100
---

# JUnit 5 테스트 지원

## 해결하는 문제 {#problem}

JUnit 5 테스트 작성 시 반복 코드를 줄여주는 확장 라이브러리입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 fixture ownership, isolation, deterministic cleanup, failure diagnostic이 필요할 때 `bluetape4k-junit5`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-junit5")
}
```

Gradle project path는 `:bluetape4k-junit5`, source directory는 `testing/junit5`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `ExtensionContext`, `AwaitilityConfigurationExtension`, `AwaitilityCoroutines`, `MultithreadingTester`, `StructuredTaskScopeTester`, `TestingExecutors`, `CancellationContracts`, `CoroutineSupport`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`ExtensionContext`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/ExtensionContext.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`ExtensionContext`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/ExtensionContext.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`AwaitilityConfigurationExtension`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityConfigurationExtension.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`AwaitilityCoroutines`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityCoroutines.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MultithreadingTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/MultithreadingTester.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`StructuredTaskScopeTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/StructuredTaskScopeTester.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`TestingExecutors`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/TestingExecutors.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CancellationContracts`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CancellationContracts.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CoroutineSupport`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CoroutineSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SuspendedJobTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/SuspendedJobTester.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`FakeValueExtension`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/faker/FakeValueExtension.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **아키텍처**, **확장 기능 구성 다이어그램**, **클래스 다이어그램**, **주요 기능**, **사용 예시**, **1. Stopwatch Extension**, **2. TempFolder Extension**, **TempFolder 주요 메소드**, **3. Output Capture**, **4. Faker 확장** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.junit.bom))
api(project(":bluetape4k-logging"))
api(project(":bluetape4k-virtualthread-api"))
testRuntimeOnly(project(":bluetape4k-virtualthread-jdk21"))
api(libs.kotlin.test.junit5)
api(libs.junit.jupiter)
api(libs.junit.jupiter.engine)
api(libs.junit.jupiter.params)
api(libs.junit.platform.launcher)
api(project(":bluetape4k-assertions"))
api(libs.mockk)
api(libs.awaitility.kotlin)
```

이 모듈의 test는 JDK 21 provider를 사용하지만, publish된 consumer는 실행 JDK에
맞는 `bluetape4k-virtualthread-jdk21` 또는 `bluetape4k-virtualthread-jdk25`
provider를 직접 추가해야 합니다. JUnit 5 helper가 consumer에 provider를
강제로 추가하지 않도록 합니다. `compileOnly` edge는 caller가 제공해야 하는
capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

모듈에서 찾은 설정 resource는 다음과 같습니다.

- [`org.junit.jupiter.api.extension.Extension`](../../../../testing/junit5/src/main/resources/META-INF/services/org.junit.jupiter.api.extension.Extension)
- [`org.junit.platform.launcher.TestExecutionListener`](../../../../testing/junit5/src/main/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener)

override하기 전에 이 resource와 binding source에서 property 이름과 default를 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

fixture를 격리하고 resource 사용량을 제한하며 diagnostic을 남기고 shared service를 확실히 닫습니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-junit5:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`ExtensionContextSupportTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/ExtensionContextSupportTest.kt)
- [`AwaitilityCoroutinesTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityCoroutinesTest.kt)
- [`MultithreadingTesterTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/concurrency/MultithreadingTesterTest.kt)
- [`StructuredTaskScopeTesterTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/concurrency/StructuredTaskScopeTesterTest.kt)
- [`CancellationContractsTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/coroutines/CancellationContractsTest.kt)
- [`CoroutineSupportTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/coroutines/CoroutineSupportTest.kt)
- [`SuspendedJobTesterTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/coroutines/SuspendedJobTesterTest.kt)
- [`DataFakerExamples`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/faker/DataFakerExamples.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### 확장 기능 구성 다이어그램 1

[![확장 기능 구성 다이어그램 1](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-01.svg)

_배포본 README: [`testing/junit5/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/junit5/README.ko.md)_

### JUnit5 클래스 구조도

[![JUnit5 클래스 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-02.svg)

_배포본 README: [`testing/junit5/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/junit5/README.ko.md)_

### Mermaid 리포트 시퀀스

[![Mermaid 리포트 시퀀스](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-03.svg)

_배포본 README: [`testing/junit5/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/junit5/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../testing/junit5/README.ko.md)
- [모듈 build](../../../../testing/junit5/build.gradle.kts)
- [`ExtensionContext`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/ExtensionContext.kt)
- [`AwaitilityConfigurationExtension`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityConfigurationExtension.kt)
- [`AwaitilityCoroutines`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityCoroutines.kt)
- [`MultithreadingTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/MultithreadingTester.kt)
- [`StructuredTaskScopeTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/StructuredTaskScopeTester.kt)
- [`TestingExecutors`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/TestingExecutors.kt)
- [`CancellationContracts`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CancellationContracts.kt)
- [`CoroutineSupport`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CoroutineSupport.kt)
- [`SuspendedJobTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/SuspendedJobTester.kt)
- [`FakeValueExtension`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/faker/FakeValueExtension.kt)
- [`ExtensionContextSupportTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/ExtensionContextSupportTest.kt)
- [`AwaitilityCoroutinesTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityCoroutinesTest.kt)
