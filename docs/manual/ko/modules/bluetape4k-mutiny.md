---
manualId: bluetape4k-mutiny
title: "Mutiny 반응형 확장"
description: "SmallRye Mutiny 반응형 라이브러리를 Kotlin에서 더 쉽게 사용할 수 있도록 확장 함수와 유틸리티를 제공합니다."
kind: library
group: concurrency
learningOrder: 210
---

# Mutiny 반응형 확장

## 해결하는 문제 {#problem}

SmallRye Mutiny 반응형 라이브러리를 Kotlin에서 더 쉽게 사용할 수 있도록 확장 함수와 유틸리티를 제공합니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 입력 계약, value semantics, algorithm cost, deterministic output이 필요할 때 `bluetape4k-mutiny`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-mutiny")
}
```

Gradle project path는 `:bluetape4k-mutiny`, source directory는 `utils/mutiny`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `CoroutineSupport`, `MultiSupport`, `UniSupport`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`CoroutineSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/CoroutineSupport.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`CoroutineSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/CoroutineSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MultiSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/MultiSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`UniSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/UniSupport.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **개요**, **의존성 추가**, **주요 기능**, **Mutiny 타입 다이어그램**, **사용 예시**, **Uni 생성**, **Uni 변환**, **Multi 생성**, **Multi 변환**, **Coroutine 연동** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(project(":bluetape4k-core"))
api(libs.mutiny)
api(libs.mutiny.kotlin)
api(project(":bluetape4k-coroutines"))
api(libs.kotlinx.coroutines.core)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

hot path를 측정하고 입력 크기를 제한하며 utility를 호출하는 application boundary에서 failure를 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-mutiny:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`CoroutineSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/CoroutineSupportTest.kt)
- [`MultiSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/MultiSupportTest.kt)
- [`UniSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/UniSupportTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Mutiny 다이어그램

[![Mutiny 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-diagram-01.svg)

_배포본 README: [`utils/mutiny/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/mutiny/README.ko.md)_

### Mutiny 다이어그램

[![Mutiny 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-diagram-02.svg)

_배포본 README: [`utils/mutiny/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/mutiny/README.ko.md)_

### Coroutine 다이어그램

[![Coroutine 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-sequence-01.svg)

_배포본 README: [`utils/mutiny/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/mutiny/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../utils/mutiny/README.ko.md)
- [모듈 build](../../../../utils/mutiny/build.gradle.kts)
- [`CoroutineSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/CoroutineSupport.kt)
- [`MultiSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/MultiSupport.kt)
- [`UniSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/UniSupport.kt)
- [`CoroutineSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/CoroutineSupportTest.kt)
- [`MultiSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/MultiSupportTest.kt)
- [`UniSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/UniSupportTest.kt)
