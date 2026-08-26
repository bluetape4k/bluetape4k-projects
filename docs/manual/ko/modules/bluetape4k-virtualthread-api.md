---
manualId: bluetape4k-virtualthread-api
title: "가상 스레드 추상화"
description: "Virtual Thread 기능을 JDK 버전에 독립적으로 사용할 수 있도록 추상화한 API 모듈입니다."
kind: library
group: concurrency
learningOrder: 220
---

# 가상 스레드 추상화

## 해결하는 문제 {#problem}

Virtual Thread 기능을 JDK 버전에 독립적으로 사용할 수 있도록 추상화한 API 모듈입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 scope ownership, cancellation, executor lifecycle, blocking boundary, shutdown이 필요할 때 `bluetape4k-virtualthread-api`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-virtualthread-api")
}
```

Gradle project path는 `:bluetape4k-virtualthread-api`, source directory는 `virtualthread/api`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `StructuredScopes`, `TaskContext`, `VirtualThreadRuntime`, `VirtualThreads`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`StructuredScopes`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/StructuredScopes.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`StructuredScopes`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/StructuredScopes.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`TaskContext`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/TaskContext.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VirtualThreadRuntime`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreadRuntime.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`VirtualThreads`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreads.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **개요**, **주요 기능**, **1. VirtualThreads - 런타임 선택 및 Executor 생성**, **2. VirtualThreadRuntime - 구현체 인터페이스**, **3. StructuredTaskScopes - 구조화된 동시성 (Structured Concurrency)**, **API 선택 가이드**, **failFast — 전체 성공 보장**, **firstSuccess — 첫 번째 성공 반환**, **supervised — 부분 실패 허용**, **getOrNull() — 안전한 결과 접근** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(project(":bluetape4k-logging"))
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

active work, queue 깊이, cancellation, timeout, executor 포화, shutdown 완료를 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-virtualthread-api:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`StructuredScopesTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/StructuredScopesTest.kt)
- [`TaskContextTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/TaskContextTest.kt)
- [`VirtualThreadsTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreadsTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### 런타임 파사드, SPI 계약, StructuredTaskScope 계약, TaskContext 도우미를 보여주는 virtualthread-api 클래스 구조

[![런타임 파사드, SPI 계약, StructuredTaskScope 계약, TaskContext 도우미를 보여주는 virtualthread-api 클래스 구조](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-api-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-api-diagram-01.svg)

_배포본 README: [`virtualthread/api/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/virtualthread/api/README.ko.md)_

### lazy provider 발견, 지원 provider 필터링, 우선순위 정렬, fallback 위임을 보여주는 ServiceLoader 기반 런타임 선택 흐름

[![lazy provider 발견, 지원 provider 필터링, 우선순위 정렬, fallback 위임을 보여주는 ServiceLoader 기반 런타임 선택 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-api-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-api-diagram-02.svg)

_배포본 README: [`virtualthread/api/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/virtualthread/api/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../virtualthread/api/README.ko.md)
- [모듈 build](../../../../virtualthread/api/build.gradle.kts)
- [`StructuredScopes`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/StructuredScopes.kt)
- [`TaskContext`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/TaskContext.kt)
- [`VirtualThreadRuntime`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreadRuntime.kt)
- [`VirtualThreads`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreads.kt)
- [`StructuredScopesTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/StructuredScopesTest.kt)
- [`TaskContextTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/TaskContextTest.kt)
- [`VirtualThreadsTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreadsTest.kt)
