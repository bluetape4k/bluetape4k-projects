---
manualId: bluetape4k-jackson2
title: "Jackson 2 직렬화"
description: "bluetape4k-jackson2은 Jackson 2.x 라이브러리를 Kotlin DSL과 확장 함수로 래핑하여 제공하는 모듈입니다."
kind: library
group: io
learningOrder: 350
---

# Jackson 2 직렬화

## 해결하는 문제 {#problem}

bluetape4k-jackson2은 Jackson 2.x 라이브러리를 Kotlin DSL과 확장 함수로 래핑하여 제공하는 모듈입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 encoding boundary, resource ownership, streaming, 호환성, malformed input이 필요할 때 `bluetape4k-jackson2`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-jackson2")
}
```

Gradle project path는 `:bluetape4k-jackson2`, source directory는 `io/jackson2`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `Jackson`, `JacksonSerializer`, `JsonGeneratorExtensions`, `JsonMapperSupport`, `JsonNodeExtensions`, `AsyncJsonParser`, `SuspendJsonParser`, `CborJacksonSerializer`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`Jackson`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/Jackson.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`Jackson`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/Jackson.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`JacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`JsonGeneratorExtensions`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonGeneratorExtensions.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`JsonMapperSupport`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonMapperSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`JsonNodeExtensions`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonNodeExtensions.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`AsyncJsonParser`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/async/AsyncJsonParser.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`SuspendJsonParser`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/async/SuspendJsonParser.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CborJacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/CborJacksonSerializer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CborJsonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/CborJsonSerializer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`IonJacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/IonJacksonSerializer.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **개요**, **bluetape4k에서 Jackson2를 쓰는 장점**, **아키텍처 다이어그램**, **클래스 구조**, **Jackson 직렬화 파이프라인**, **필드 암호화 흐름 (@JsonTinkEncrypt)**, **추천 사용 시나리오**, **Anti-Patterns**, **주요 기능**, **1. JsonMapper DSL** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.jackson.bom))
implementation(platform(libs.spring.boot.dependencies))
api(libs.jackson.core)
api(libs.jackson.databind)
api(libs.jackson.datatype.jdk8)
api(libs.jackson.datatype.jsr310)
api(libs.jackson.module.kotlin)
api(libs.jackson.module.parameter.names)
api(libs.jackson.module.blackbird)
compileOnly(libs.jackson.dataformat.properties)
compileOnly(libs.jackson.dataformat.yaml)
compileOnly(libs.jackson.dataformat.avro)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

모듈에서 찾은 설정 resource는 다음과 같습니다.

- [`com.fasterxml.jackson.databind.Module`](../../../../io/jackson2/src/main/resources/META-INF/services/com.fasterxml.jackson.databind.Module)

override하기 전에 이 resource와 binding source에서 property 이름과 default를 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

payload 크기, allocation, latency, malformed input 비율, resource close, protocol 오류를 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-jackson2:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`DisallowedTypedPayload`](../../../../io/jackson2/src/test/kotlin/com/example/disallowed/DisallowedTypedPayload.kt)
- [`AbstractJsonSerializerTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/AbstractJsonSerializerTest.kt)
- [`JacksonSerializerTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JacksonSerializerTest.kt)
- [`JacksonTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JacksonTest.kt)
- [`JsonGeneratorExtensionsTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JsonGeneratorExtensionsTest.kt)
- [`JsonMapperSupportTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JsonMapperSupportTest.kt)
- [`JsonNodeExtensionsTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JsonNodeExtensionsTest.kt)
- [`AsyncJsonParserTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/async/AsyncJsonParserTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Jackson2 클래스 구조 다이어그램

[![Jackson2 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-diagram-01.svg)

_배포본 README: [`io/jackson2/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/jackson2/README.ko.md)_

### Jackson2 직렬화 파이프라인 다이어그램

[![Jackson2 직렬화 파이프라인 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-diagram-02.svg)

_배포본 README: [`io/jackson2/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/jackson2/README.ko.md)_

### JsonTinkEncrypt 필드 암호화 시퀀스 다이어그램

[![JsonTinkEncrypt 필드 암호화 시퀀스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-sequence-01.svg)

_배포본 README: [`io/jackson2/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/jackson2/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../io/jackson2/README.ko.md)
- [모듈 build](../../../../io/jackson2/build.gradle.kts)
- [`Jackson`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/Jackson.kt)
- [`JacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt)
- [`JsonGeneratorExtensions`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonGeneratorExtensions.kt)
- [`JsonMapperSupport`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonMapperSupport.kt)
- [`JsonNodeExtensions`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonNodeExtensions.kt)
- [`AsyncJsonParser`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/async/AsyncJsonParser.kt)
- [`SuspendJsonParser`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/async/SuspendJsonParser.kt)
- [`CborJacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/CborJacksonSerializer.kt)
- [`CborJsonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/CborJsonSerializer.kt)
- [`IonJacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/IonJacksonSerializer.kt)
- [`DisallowedTypedPayload`](../../../../io/jackson2/src/test/kotlin/com/example/disallowed/DisallowedTypedPayload.kt)
- [`AbstractJsonSerializerTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/AbstractJsonSerializerTest.kt)
