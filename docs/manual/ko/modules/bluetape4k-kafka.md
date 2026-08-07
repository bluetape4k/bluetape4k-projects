---
manualId: bluetape4k-kafka
title: "Kafka 클라이언트 확장"
description: "Kafka 3.x client, coroutine producer, Spring Kafka·Reactor Kafka adapter와 Kafka Streams Kotlin factory를 1.12.1 소스 기준으로 설명합니다."
kind: library
group: messaging
learningOrder: 700
---

# Kafka 클라이언트 확장

## 제공하는 기능 {#problem}

`bluetape4k-kafka`는 Kafka를 새로 추상화하는 messaging framework가 아닙니다. Kafka 3.x client의 producer·consumer 생성, 레코드와 `TopicPartition` helper, serializer와 deserializer를 묶은 codec, coroutine 발송, Spring Kafka·Reactor Kafka adapter, Kafka Streams parameter factory를 한 artifact에 모읍니다.

broker protocol, partition 배치, consumer group, delivery guarantee와 retry 정책은 Kafka와 각 client framework가 그대로 맡습니다. 이 매뉴얼은 helper가 줄여 주는 코드와 애플리케이션이 계속 책임져야 하는 경계를 함께 다룹니다.

## 사용하기 전에 결정할 것 {#when-to-use}

다음 질문부터 답합니다.

- Kafka 3.x/Spring Kafka 3.x 계열인가, Kafka 4.x/Spring Kafka 4.x 계열인가?
- native `Producer`만 coroutine으로 기다리면 되는가, Spring `KafkaOperations`나 Reactor Kafka template까지 필요한가?
- payload type을 header에서 복원할 것인가? 그렇다면 어떤 package만 허용할 것인가?
- consumer offset을 자동, 수동 acknowledgment, transaction 중 어느 방식으로 관리할 것인가?
- Kafka Streams를 쓴다면 runtime에 `kafka-streams`를 직접 제공했는가?

Kafka 4.x line은 [`bluetape4k-kafka4`](./bluetape4k-kafka4.md)입니다. 두 artifact는 같은 package와 class 이름을 공유하므로 한 애플리케이션에 함께 넣지 않습니다.

## 의존성 추가 {#coordinates}

사용자는 생태계 BOM 버전 하나만 선택하면 됩니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-kafka")
}
```

Gradle project path는 `:bluetape4k-kafka`, source directory는 `infra/kafka`입니다. artifact별 버전을 따로 적지 말고 `bluetape4k-dependencies`에서 맞춘 조합을 사용합니다.

## 학습 경로 {#concepts}

이 매뉴얼은 기능 목록이 아니라 실제 작업 순서로 읽도록 구성했습니다.

1. [모듈 경계와 client·record helper](./bluetape4k-kafka/module-boundary-client-records.md)에서 Kafka 3.x line, resource ownership과 작은 factory를 확인합니다.
2. [Codec, wire format과 보안](./bluetape4k-kafka/codecs-wire-format-security.md)에서 type header allowlist, poison pill과 선택형 binary backend를 결정합니다.
3. [Coroutine producer](./bluetape4k-kafka/coroutine-producer.md)에서 callback, cancellation, `Flow` concurrency와 `flush` 시점을 익힙니다.
4. [Spring Kafka template과 listener adapter](./bluetape4k-kafka/spring-kafka-templates-listeners.md)에서 두 `suspendSend` 계열과 Reactor Kafka template의 차이를 구분합니다.
5. [Kafka Streams DSL factory](./bluetape4k-kafka/kafka-streams-dsl.md)에서 serde, state store와 topology ownership을 확인합니다.
6. [실패, 테스트, 운영과 생태계](./bluetape4k-kafka/failures-testing-operations-ecosystem.md)에서 delivery, offset, 지표, `kafka4`와 `kafka-logback` 경계를 정리합니다.

각 장에는 바로 실행할 수 있는 예제와 source·test 링크가 있습니다. 앞의 세 장으로 producer와 wire format을 먼저 고정하고, 사용하는 framework에 맞춰 Spring 또는 Streams 장으로 넘어가는 편이 좋습니다.

## 첫 producer {#quick-start}

```kotlin
val producer = producerOf(
    configs = mapOf(
        "bootstrap.servers" to bootstrapServers,
        "acks" to "all",
        "key.serializer" to StringSerializer::class.java,
        "value.serializer" to StringSerializer::class.java,
    ),
)

producer.use {
    val metadata = it.suspendSend(ProducerRecord("orders", "order-1", "created"))
    println("partition=${metadata.partition()}, offset=${metadata.offset()}")
}
```

`producerOf`는 새 `KafkaProducer`를 만들 뿐 singleton이나 Spring bean으로 등록하지 않습니다. 직접 만든 producer는 호출자가 닫습니다. `suspendSend`는 callback 결과를 기다리고 coroutine이 취소되면 Kafka `Future.cancel(true)`를 호출하지만, broker가 이미 받은 record까지 취소됐다고 보장하지는 않습니다.

## API 선택 지도 {#api-by-task}

| 작업 | 시작 API | 다음 장 |
| --- | --- | --- |
| native producer·consumer 생성 | `producerOf`, `consumerOf` | [모듈 경계](./bluetape4k-kafka/module-boundary-client-records.md) |
| metric·topic-partition helper | `getMetricValueOrNull`, `topicPartitionOf` | [모듈 경계](./bluetape4k-kafka/module-boundary-client-records.md) |
| 문자열·byte array·JSON·binary codec | `KafkaCodec`, `KafkaCodecs` | [Codec과 보안](./bluetape4k-kafka/codecs-wire-format-security.md) |
| native producer를 suspend/Flow로 사용 | `suspendSend`, `sendAsFlow`, `sendAndForget` | [Coroutine producer](./bluetape4k-kafka/coroutine-producer.md) |
| Spring `KafkaOperations` 발송 | `io.bluetape4k.kafka.spring.suspendSend` | [Spring 연동](./bluetape4k-kafka/spring-kafka-templates-listeners.md) |
| Reactor Kafka producer·consumer | `SuspendKafkaProducerTemplate`, `SuspendKafkaConsumerTemplate` | [Spring 연동](./bluetape4k-kafka/spring-kafka-templates-listeners.md) |
| Streams parameter 구성 | `consumedOf`, `materializedOf`, `streamJoinedOf` 등 | [Streams DSL](./bluetape4k-kafka/kafka-streams-dsl.md) |

## 권장 패턴 {#patterns}

- producer와 consumer는 애플리케이션 컴포넌트의 수명주기에 맞춰 한 번 만들고 닫습니다.
- wire format과 type allowlist를 topic 계약으로 문서화하고 producer·consumer 양쪽에서 호환성 테스트를 실행합니다.
- `suspendSend`가 성공해야 business transaction이 성공하는지, 별도 outbox로 전달할지 먼저 결정합니다.
- consumer 처리와 offset commit의 순서를 명시합니다. exactly-once batch는 caller가 transaction을 commit 또는 abort합니다.
- `Flow` helper의 동시성과 buffer를 처리량 요구에 맞춰 제한하고, key ordering이 필요한 workload는 partition 안의 순서를 검증합니다.
- topic, consumer group, error class처럼 cardinality가 제한된 값을 metric label로 사용합니다. record key와 payload는 label에 넣지 않습니다.

## 연동 {#integrations}

1.12.1 build는 Kafka client를 API로, Spring Kafka와 Reactor Kafka를 implementation으로 사용합니다. Kafka Streams, Spring Kafka test, resilience4j, Kryo·Fory와 일부 compressor는 선택형 edge입니다. 해당 API를 호출하면 애플리케이션 runtime에도 그 dependency가 있어야 합니다.

`bluetape4k-kafka`는 Kafka 3.9.x/Spring Kafka 3.x/Jackson 2 line입니다. Kafka 4.2.x/Spring Kafka 4.x/Jackson 3 line은 `bluetape4k-kafka4`를 선택합니다. Logback event를 Kafka로 내보내는 기능은 별도 [`bluetape4k-kafka-logback`](./bluetape4k-kafka-logback.md) artifact가 맡습니다.

## 설정 {#configuration}

main resource나 auto-configuration은 없습니다. native client는 `Map` 또는 `Properties`, Reactor Kafka template은 `SenderOptions`·`ReceiverOptions`, Spring Kafka는 application이 구성한 `KafkaOperations`와 listener container를 사용합니다.

serializer, idempotence, acknowledgments, transaction id, consumer group, offset reset, poll interval, security protocol과 TLS/SASL은 Kafka client 설정입니다. helper가 안전한 기본값으로 덮어쓰지 않습니다.

## 실패 동작 {#failures}

native producer와 Spring 발송 adapter는 callback 또는 future의 예외를 suspend 호출자에게 전달합니다. coroutine 취소는 local wait와 future 취소를 시도할 뿐 전달 여부를 판정하지 못합니다. 재시도 전에 idempotent producer와 업무 idempotency를 함께 설계합니다.

`AbstractKafkaCodec.deserialize`는 일반 `Exception`을 log한 뒤 `null`을 반환합니다. `CancellationException`과 JVM `Error`는 다시 던집니다. `null`을 정상 payload와 구분하고 `ErrorHandlingDeserializer`, DLQ와 metric으로 poison pill을 추적해야 합니다.

## 운영 {#operations}

producer의 send error·retry·request latency·buffer available bytes, consumer lag·rebalance·poll interval·commit failure, Streams state store와 restore 상태를 관찰합니다. shutdown 순서는 새 입력 중단, in-flight 처리와 commit/flush, client close 순으로 애플리케이션이 정합니다.

1.12.1은 취약한 `org.lz4:lz4-java`를 제외하고 호환 namespace의 `at.yawk.lz4` 구현을 API로 노출합니다. 의존성 tree에서 예전 artifact가 다시 들어오지 않는지 배포 전에 확인합니다.

## 테스트 {#testing}

server 없는 helper test부터 실행하고 Kafka가 필요한 통합 test는 순차 실행합니다.

```bash
./gradlew :bluetape4k-kafka:test \
  --tests "io.bluetape4k.kafka.TopicPartitionSupportTest" \
  --tests "io.bluetape4k.kafka.codec.*" \
  --tests "io.bluetape4k.kafka.streams.kstream.KStreamDslTest" \
  --no-configuration-cache
```

전체 task는 Testcontainers Kafka를 시작하는 test를 포함합니다.

```bash
./gradlew :bluetape4k-kafka:test --no-configuration-cache
```

## 워크숍 {#workshops}

전용 workshop은 manual manifest에 등록되어 있지 않습니다. README 예제로 API 모양을 확인한 뒤, `coroutines`, `spring/core`, `streams/kstream` test를 실행 가능한 학습 자료로 사용합니다. 애플리케이션에서는 production과 같은 serializer, security와 topic 설정으로 별도 통합 test를 둡니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 release `1.12.1`의 commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`를 설명합니다. 현재 개발 branch에는 Kafka test 임시 directory 격리와 `SuspendKafkaConsumerTemplate.close()` 진단·예외 처리 개선이 추가되어 있습니다. 1.12.1에서는 `AutoCloseable` receiver만 닫고, 아닌 경우 별도 경고가 없으며, `close()` 예외도 직접 전파됩니다.

## Source와 tests {#sources}

- [모듈 README](../../../../infra/kafka/README.ko.md)
- [1.12.1 build](../../../../infra/kafka/build.gradle.kts)
- [`ProducerCoroutines.kt`](../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/coroutines/ProducerCoroutines.kt)
- [`KafkaCodec.kt`](../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt)
- [`SuspendKafkaProducerTemplate.kt`](../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/spring/core/SuspendKafkaProducerTemplate.kt)
- [`SuspendKafkaConsumerTemplate.kt`](../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/spring/core/SuspendKafkaConsumerTemplate.kt)
- [`KStreamDslTest.kt`](../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/streams/kstream/KStreamDslTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Kafka API 구조

[![Kafka API 구조](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-diagram-01.svg)

_배포본 README: [`infra/kafka/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/kafka/README.ko.md)_

### Kafka Streams 다이어그램

[![Kafka Streams 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-diagram-02.svg)

_배포본 README: [`infra/kafka/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/kafka/README.ko.md)_

### Producer/Consumer 다이어그램

[![Producer/Consumer 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-sequence-01.svg)

_배포본 README: [`infra/kafka/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/kafka/README.ko.md)_

<!-- release-readme-diagrams:end -->
