---
title: Spring Kafka template과 listener adapter
description: KafkaOperations await extension, Reactor Kafka 기반 suspend template, consumer offset·transaction과 listener utility의 경계를 설명합니다.
manualId: bluetape4k-kafka
chapterId: spring-kafka-templates-listeners
---

# Spring Kafka template과 listener adapter

## 세 가지 발송 진입점

모듈에는 이름이 비슷한 세 계층이 있습니다.

| API | 기반 | 반환 |
| --- | --- | --- |
| `io.bluetape4k.kafka.spring.suspendSend` | Spring `KafkaOperations.send(...).await()` | `SendResult` |
| `io.bluetape4k.kafka.spring.core.suspendSend` | `KafkaOperations.execute`와 producer callback | `SendResult` |
| `SuspendKafkaProducerTemplate` | Reactor Kafka `KafkaSender` | `SenderResult`·`Flow` |

일반 `KafkaTemplate`을 이미 bean으로 사용한다면 첫 번째 extension이 가장 작습니다.

```kotlin
val result = kafkaTemplate.suspendSend("orders", order.id, order)
```

`spring.core`의 Flow helper는 1.12.1에서 `onCompletion { flush() }`를 사용합니다. 정상 완료뿐 아니라 exception·cancellation에서도 flush가 호출될 수 있습니다. 이 동작이 필요하지 않다면 native coroutine helper 또는 개별 `suspendSend`를 사용합니다.

## Reactor Kafka producer template

`SuspendKafkaProducerTemplate`은 application이 만든 `SenderOptions` 또는 `KafkaSender`를 받습니다. 단일 record, `Flow<SenderRecord>`, Spring `Message`와 transaction 발송을 제공합니다.

```kotlin
val template = SuspendKafkaProducerTemplate(senderOptions)
try {
    template.send("orders", order.id, order)
} finally {
    template.close()
}
```

Spring `Message` 변환 결과가 `ProducerRecord`가 아니면 명확한 `IllegalArgumentException`을 던지고 correlation id header를 복사합니다. close는 내부 scope를 취소하고 sender를 닫으며 sender close 예외는 log한 뒤 삼킵니다.

## Reactor Kafka consumer template

`SuspendKafkaConsumerTemplate`은 `receive`, `receiveAutoAck`, `receiveExactlyOnce`와 subscribe·assign·seek·commit·pause·metrics 등의 `Consumer` operation을 suspend 함수로 노출합니다.

```kotlin
template.receive().collect { record ->
    handle(record.value())
    record.receiverOffset().acknowledge()
}
```

`receiveAutoAck`는 batch inner publisher를 이어 붙입니다. 처리가 끝나기 전에 offset이 commit되는지 등 세부 의미는 Reactor Kafka 설정과 API 계약을 확인합니다. 수동 처리에서는 성공한 record의 offset만 acknowledge합니다.

## exactly-once와 offset

`receiveExactlyOnce(transactionManager)`는 transaction별 inner `Flow`를 반환합니다. 모듈이 업무 처리를 자동 commit하지 않습니다. caller가 각 batch 처리 뒤 `commit()`, 실패 시 `abort()`를 실행해야 다음 batch로 진행합니다.

`commitCurrentOffsets`는 현재 position을 `OffsetAndMetadata`로 만들고 `commitSync`합니다. 요청한 partition이 현재 assignment에 없으면 `IllegalArgumentException`입니다. `committed`와 `offsetsForTimes` map의 값은 Kafka가 null을 반환할 수 있어 nullable입니다.

## 1.12.1 종료 경계

consumer template close는 scope를 취소한 다음 receiver가 `AutoCloseable`일 때만 닫습니다. `AutoCloseable`이 아니면 아무 경고도 없고, close가 예외를 던지면 호출자에게 그대로 전파됩니다. 현재 branch의 경고와 예외 흡수는 1.12.1 이후 변경이라 이 release 계약에 포함하지 않습니다.

## listener adapter는 container를 만들지 않는다

`listenerTypeOf`, `stoppableSleep`, `createOffsetAndMetadata`, `consumerRecordMetadataOf`는 Spring Kafka utility에 위임하는 작은 함수입니다. `@KafkaListener`, container factory, error handler나 retry topic을 자동 구성하지 않습니다.

## Source와 tests

- [`KafkaOperationsExtensions.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/spring/KafkaOperationsExtensions.kt)
- [`KafkaOperationExtensions.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/spring/core/KafkaOperationExtensions.kt)
- [`SuspendKafkaProducerTemplate.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/spring/core/SuspendKafkaProducerTemplate.kt)
- [`SuspendKafkaConsumerTemplate.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/spring/core/SuspendKafkaConsumerTemplate.kt)
- [`SuspendKafkaConsumerTemplateTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/spring/core/SuspendKafkaConsumerTemplateTest.kt)
- [`ListenerUtilsTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/spring/listener/ListenerUtilsTest.kt)
