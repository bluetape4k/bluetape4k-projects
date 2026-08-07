---
title: Producer와 coroutine 발행
description: Producer DSL, 단건·메시지 builder 발행, 순차 Flow와 취소·실패 경계를 설명합니다.
manualId: bluetape4k-pulsar
chapterId: producers-coroutine-send
---

# Producer와 coroutine 발행

## Producer 생성

`client.producer(schema) { ... }`는 native `newProducer(schema)` builder에 setup을 적용하고 동기 `create()`로 Producer를 만듭니다.

```kotlin
val producer = client.producer(Schema.STRING) {
    topic("persistent://public/default/orders")
    producerName("order-api")
    compressionType(CompressionType.LZ4)
}
```

topic, routing, batching, pending queue와 timeout 검증은 Pulsar Client가 담당합니다. helper는 default를 바꾸지 않습니다.

## 두 단건 발행 API

값만 보내려면 `sendSuspend(message)`, key·property·event time 같은 metadata가 필요하면 message builder overload를 사용합니다.

```kotlin
val first = producer.sendSuspend(order)

val second = producer.sendSuspend {
    value(order)
    key(order.id)
    property("source", "order-api")
}
```

둘 다 `sendAsync()` 결과를 `awaitSuspending()`으로 기다리고 broker가 반환한 `MessageId`를 돌려줍니다. Pulsar exception을 domain exception으로 변환하지 않습니다.

## sendAsFlow는 순차 처리

`sendAsFlow(messages)`는 upstream `Flow<T>`를 collect하면서 각 `sendAsync`가 끝난 뒤 `MessageId`를 emit합니다.

```kotlin
val ids = producer.sendAsFlow(orders.asFlow()).toList()
```

이 구조는 입력 순서대로 한 번에 하나를 기다립니다. `Flow`라는 이유만으로 parallel send나 batching이 생기지 않습니다. throughput이 필요하면 Pulsar producer batching 설정과 제한된 동시성을 따로 설계하고 순서 요구사항을 먼저 확인합니다.

## cold Flow와 재수집

반환된 Flow는 cold입니다. collect할 때마다 upstream을 다시 collect하고 메시지를 다시 발행합니다. 같은 Flow를 두 번 collect하면 동일 업무 메시지가 두 번 전송될 수 있으므로 재수집 여부를 호출부에서 통제합니다.

발행 재시도도 중복을 만들 수 있습니다. message key만으로 exactly-once가 생기지 않으며 consumer idempotency, Pulsar deduplication 또는 transaction 정책을 애플리케이션 수준에서 결정합니다.

## 취소와 실패

`sendAsFlow`는 await 중 `CancellationException`을 받으면 해당 future에 `cancel(true)`를 호출하고 취소를 다시 던집니다. broker가 이미 메시지를 받아들였다면 이 호출이 발행을 되돌린다고 가정할 수 없습니다.

한 메시지 발행이 실패하면 Flow는 즉시 종료하고 뒤 메시지는 발행하지 않습니다. 어느 메시지까지 broker에 저장됐는지 확인할 수 있도록 안정적인 업무 ID와 발행 결과를 기록합니다.

## 수명주기와 운영

`withProducer`는 짧은 scope에서 close를 시도하지만 1.12.1은 취소 불가능한 cleanup을 보장하지 않습니다. 장기 producer는 재사용하고 send latency, pending queue, batching, compression과 failure rate를 관찰합니다.

## Source와 tests

- [`ProducerSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerSupport.kt)
- [`ProducerExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerExtensions.kt)
- [`ProducerSupportTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/producer/ProducerSupportTest.kt)
- [`ProducerExtensionsTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/producer/ProducerExtensionsTest.kt)
