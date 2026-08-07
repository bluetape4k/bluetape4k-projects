---
manualId: bluetape4k-kafka4
title: "Kafka 4 클라이언트 확장"
description: "bluetape4k-kafka4는 bluetape4k Kafka 유틸리티의 Kafka 4.x 라인입니다. 기존 bluetape4k-kafka와 같은 Kotlin 우선 API 형태를 유지하되 Kafka 4.2.x, Spring Kafka 4.x, Spring Boot 4, Jackson 3 기준으로 컴파일합니다."
kind: library
group: messaging
learningOrder: 710
---

# Kafka 4 클라이언트 확장

## 해결하는 문제 {#problem}

bluetape4k-kafka4는 bluetape4k Kafka 유틸리티의 Kafka 4.x 라인입니다. 기존 bluetape4k-kafka와 같은 Kotlin 우선 API 형태를 유지하되 Kafka 4.2.x, Spring Kafka 4.x, Spring Boot 4, Jackson 3 기준으로 컴파일합니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 client lifecycle, reconnect policy, backpressure, retry, observability이 필요할 때 `bluetape4k-kafka4`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-kafka4")
}
```

Gradle project path는 `:bluetape4k-kafka4`, source directory는 `infra/kafka4`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `ConsumerSupport`, `ProducerSupport`, `TopicPartitionSupport`, `BinaryKafkaCodecs`, `ByteArrayKafkaCodec`, `JacksonKafkaCodec`, `KafkaCodec`, `KafkaCodecs`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`ConsumerSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/ConsumerSupport.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`ConsumerSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/ConsumerSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ProducerSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/ProducerSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`TopicPartitionSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/TopicPartitionSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`BinaryKafkaCodecs`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecs.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ByteArrayKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/ByteArrayKafkaCodec.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`JacksonKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/JacksonKafkaCodec.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`KafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`KafkaCodecs`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodecs.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`StringKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/StringKafkaCodec.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ProducerCoroutines`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/coroutines/ProducerCoroutines.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **호환성**, **특징**, **의존성**, **Gradle Kotlin DSL**, **Maven**, **의존성 경계**, **Producer**, **Coroutine Producer**, **Spring Kafka**, **Jackson 3 Codec** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-annotations"))
api(project(":bluetape4k-core"))
api(project(":bluetape4k-io"))
compileOnly(project(":bluetape4k-resilience4j"))
api(libs.kafka4.clients)
compileOnly(libs.kafka4.streams)
compileOnly(libs.kafka4.generator)
implementation(libs.spring.kafka4)
compileOnly(libs.spring.kafka4.test)
implementation(project(":bluetape4k-spring-boot-core"))
implementation("org.springframework.data:spring-data-commons")
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

connection 상태, queue 깊이, retry, timeout, remote 오류, graceful shutdown을 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-kafka4:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractKafkaTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/AbstractKafkaTest.kt)
- [`ConsumerSupportTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/ConsumerSupportTest.kt)
- [`ProducerSupportTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/ProducerSupportTest.kt)
- [`TopicPartitionSupportTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/TopicPartitionSupportTest.kt)
- [`AbstractKafkaCodecPoisonPillTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/AbstractKafkaCodecPoisonPillTest.kt)
- [`AbstractKafkaCodecTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/AbstractKafkaCodecTest.kt)
- [`ByteArrayKafkaCodecTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/ByteArrayKafkaCodecTest.kt)
- [`JacksonKafkaCodecSecurityTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/JacksonKafkaCodecSecurityTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Kafka4 의존성 경계 다이어그램

[![Kafka4 의존성 경계 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka4-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka4-diagram-01.svg)

_배포본 README: [`infra/kafka4/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/kafka4/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../infra/kafka4/README.ko.md)
- [모듈 build](../../../../infra/kafka4/build.gradle.kts)
- [`ConsumerSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/ConsumerSupport.kt)
- [`ProducerSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/ProducerSupport.kt)
- [`TopicPartitionSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/TopicPartitionSupport.kt)
- [`BinaryKafkaCodecs`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecs.kt)
- [`ByteArrayKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/ByteArrayKafkaCodec.kt)
- [`JacksonKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/JacksonKafkaCodec.kt)
- [`KafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt)
- [`KafkaCodecs`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodecs.kt)
- [`StringKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/StringKafkaCodec.kt)
- [`ProducerCoroutines`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/coroutines/ProducerCoroutines.kt)
- [`AbstractKafkaTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/AbstractKafkaTest.kt)
- [`ConsumerSupportTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/ConsumerSupportTest.kt)
