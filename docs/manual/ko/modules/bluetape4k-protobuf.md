---
manualId: bluetape4k-protobuf
title: "Protocol Buffers 직렬화"
description: "Google Protocol Buffers 메시지 처리를 위한 Kotlin 확장 라이브러리입니다."
kind: library
group: io
learningOrder: 380
---

# Protocol Buffers 직렬화

## 해결하는 문제 {#problem}

Google Protocol Buffers 메시지 처리를 위한 Kotlin 확장 라이브러리입니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 encoding boundary, resource ownership, streaming, 호환성, malformed input이 필요할 때 `bluetape4k-protobuf`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-protobuf")
}
```

Gradle project path는 `:bluetape4k-protobuf`, source directory는 `io/protobuf`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `DateTimeSupport`, `DurationSupport`, `MessageSupport`, `MoneySupport`, `TimestampSupport`, `TypeAlias`, `ProtobufSerializer`, `LettuceProtobufCodecs`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`DateTimeSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DateTimeSupport.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`DateTimeSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DateTimeSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`DurationSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DurationSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MessageSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MessageSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MoneySupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MoneySupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`TimestampSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/TimestampSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`TypeAlias`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/TypeAlias.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ProtobufSerializer`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceProtobufCodecs`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecs.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonProtobufCodec`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodec.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`RedissonProtobufCodecs`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodecs.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **개요**, **아키텍처**, **Protobuf 클래스 구조**, **Protobuf 타입 변환 흐름**, **ProtobufSerializer 허용 목록 시퀀스**, **주요 기능**, **보안: ProtobufSerializer 허용 목록**, **사용 예시**, **1. 타입 별칭**, **2. Timestamp 변환** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

Lettuce의 strict uncompressed factory는 기본 prefix Protobuf message를 caller-owned `ByteBuf`에 기록합니다.
Trusted-internal factory는 같은 target 경로와 legacy fallback을 함께 사용하므로 shared/untrusted boundary에서는
금지합니다. 압축, custom-prefix, fallback, 단일 인자의 `ByteBuffer` 경로는 copied 동작을 유지합니다. 실패한
target write는 `writerIndex`를 commit하지 않지만 caller는 시도한 bytes/capacity 변경을 clear하거나 buffer를
폐기해야 합니다. 기존 factory caller는 migration이 없으며 Java에서는
`LettuceProtobufCodecs.INSTANCE.protobuf()`를 사용합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
api(libs.protobuf.java)
api(libs.protobuf.java.util)
api(libs.protobuf.kotlin)
api(libs.proto.google.common.protos)
api(project(":bluetape4k-io"))
compileOnly(project(":bluetape4k-lettuce"))
compileOnly(project(":bluetape4k-redisson"))
compileOnly(libs.lz4.java)
compileOnly(libs.snappy.java)
compileOnly(libs.zstd.jni)
compileOnly(project(":bluetape4k-money"))
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
./gradlew :bluetape4k-protobuf:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`DateTimeSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/DateTimeSupportTest.kt)
- [`DurationSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/DurationSupportTest.kt)
- [`DynamicMessageExamples`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/DynamicMessageExamples.kt)
- [`MessageSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/MessageSupportTest.kt)
- [`MoneySupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/MoneySupportTest.kt)
- [`TimestampSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/TimestampSupportTest.kt)
- [`ProtobufSerializerSecurityTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerSecurityTest.kt)
- [`ProtobufSerializerTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Protobuf 클래스 구조 다이어그램

[![Protobuf 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-protobuf-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-protobuf-diagram-01.svg)

_배포본 README: [`io/protobuf/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/protobuf/README.ko.md)_

### Protobuf 타입 변환 흐름 다이어그램

[![Protobuf 타입 변환 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-protobuf-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-protobuf-diagram-02.svg)

_배포본 README: [`io/protobuf/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/protobuf/README.ko.md)_

### ProtobufSerializer 허용 목록 검증 시퀀스 다이어그램

[![ProtobufSerializer 허용 목록 검증 시퀀스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-protobuf-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-protobuf-sequence-01.svg)

_배포본 README: [`io/protobuf/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/protobuf/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../io/protobuf/README.ko.md)
- [모듈 build](../../../../io/protobuf/build.gradle.kts)
- [`DateTimeSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DateTimeSupport.kt)
- [`DurationSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DurationSupport.kt)
- [`MessageSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MessageSupport.kt)
- [`MoneySupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MoneySupport.kt)
- [`TimestampSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/TimestampSupport.kt)
- [`TypeAlias`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/TypeAlias.kt)
- [`ProtobufSerializer`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt)
- [`LettuceProtobufCodecs`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecs.kt)
- [`RedissonProtobufCodec`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodec.kt)
- [`RedissonProtobufCodecs`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodecs.kt)
- [`DateTimeSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/DateTimeSupportTest.kt)
- [`DurationSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/DurationSupportTest.kt)
