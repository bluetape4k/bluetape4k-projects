---
title: Coroutine producer
description: native Producer callback을 suspend로 바꾸는 방식, cancellation, Flow concurrency와 flush의 정상 완료 경계를 설명합니다.
manualId: bluetape4k-kafka
chapterId: coroutine-producer
---

# Coroutine producer

## callback을 기다리는 `suspendSend`

`Producer.suspendSend(record)`는 `suspendCancellableCoroutine`에서 Kafka callback을 기다립니다. callback exception은 호출자에게 전달하고 성공하면 `RecordMetadata`를 반환합니다.

```kotlin
val metadata = producer.suspendSend(
    ProducerRecord("orders", order.id, order)
)
```

coroutine이 대기 중 취소되면 반환된 Kafka `Future`에 `cancel(true)`를 호출합니다. 이 동작은 local 대기를 중단하려는 시도입니다. broker가 record를 쓰지 않았다는 확인이 아니므로 timeout이나 취소 뒤 같은 업무 event를 다시 보낼 때 duplicate를 고려합니다.

## `sendAsFlow`

`sendAsFlow(records)`는 input에 `buffer()`를 붙이고 bluetape4k `Flow.async`로 각 `suspendSend`를 실행해 `Flow<RecordMetadata>`를 반환합니다.

```kotlin
producer.sendAsFlow(records).collect { metadata ->
    audit(metadata.topic(), metadata.partition(), metadata.offset())
}
```

동시 실행 정도와 ordering은 `Flow.async`의 계약까지 함께 확인해야 합니다. Kafka는 같은 partition에 실제로 기록된 순서를 보존하지만, 여러 send의 completion 순서나 여러 partition의 전역 순서를 보장하지 않습니다. 업무 순서가 중요하면 같은 key/partition을 사용하고 test에서 completion과 consume 결과를 모두 확인합니다.

## 마지막 결과만 받기

`sendAsFlowParallel(records)`도 buffered async send를 수행한 뒤 `.last()`를 반환합니다. input이 비어 있으면 `NoSuchElementException`이 발생합니다. 마지막 metadata만으로 앞선 모든 record의 업무 결과를 표현하지 말고, 하나라도 실패하면 전체 collection이 실패한다는 점을 caller contract에 적습니다.

## fire-and-forget의 실제 의미

`sendAndForget`이라는 이름과 달리 함수는 flow를 끝까지 collect하고 각 `suspendSend` 결과를 기다립니다. 단지 metadata를 반환하지 않을 뿐입니다. 실패도 호출자에게 전파됩니다.

```kotlin
producer.sendAndForget(records, needFlush = true)
```

1.12.1 구현은 정상 완료일 때만 선택적으로 `flush()`합니다. exception이나 cancellation 중에는 이미 실패한 send를 기다리며 block하거나 원래 오류를 가리지 않도록 flush하지 않습니다. client close 여부는 여전히 호출자 책임입니다.

## 처리량과 backpressure

buffer와 async send는 순차 loop보다 처리량을 높일 수 있지만 producer 내부 buffer, `max.in.flight.requests.per.connection`과 application memory가 함께 늘 수 있습니다. 큰 upstream을 무제한으로 연결하지 말고 concurrency, timeout과 producer buffer metric을 측정합니다.

## Source와 tests

- [`ProducerCoroutines.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/coroutines/ProducerCoroutines.kt)
- [`ProducerSupportTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/coroutines/ProducerSupportTest.kt)

test는 callback exception 전파와 coroutine 취소 시 future cancel을 server 없이 검증하고, 실제 Kafka 발송 Flow는 Testcontainers로 확인합니다.
