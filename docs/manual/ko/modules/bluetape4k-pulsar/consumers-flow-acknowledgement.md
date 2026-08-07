---
title: Consumer, Flow와 ack
description: subscription 구성, 무한 수신 Flow, 개별·누적 ack와 처리 실패 경계를 설명합니다.
manualId: bluetape4k-pulsar
chapterId: consumers-flow-acknowledgement
---

# Consumer, Flow와 ack

## subscription을 먼저 설계한다

Consumer는 topic뿐 아니라 subscription name과 type이 처리 의미를 결정합니다.

```kotlin
val consumer = client.consumer(orderSchema) {
    topic("persistent://public/default/orders")
    subscriptionName("order-fulfillment")
    subscriptionType(SubscriptionType.Shared)
}
```

`consumer` helper는 native builder에 setup을 적용하고 동기 `subscribe()`를 호출합니다. subscription 생성, receiver queue, ack timeout과 dead-letter policy는 Pulsar 설정입니다.

## 단건 수신과 개별 ack

`receiveSuspend()`는 `receiveAsync()`를 기다립니다. 업무 처리가 성공한 뒤 `acknowledgeSuspend(message)`를 호출합니다.

```kotlin
val message = consumer.receiveSuspend()
process(message.value)
consumer.acknowledgeSuspend(message)
```

process 뒤 ack 전에 장애가 나면 redelivery될 수 있습니다. handler가 같은 메시지를 다시 처리해도 안전하도록 message ID나 업무 key를 기준으로 idempotency를 설계합니다.

## receiveAsFlow의 수명

`receiveAsFlow()`는 coroutine context가 active인 동안 `receiveAsync()`를 반복하는 cold Flow입니다. 메시지가 없다고 정상 종료하지 않고 다음 메시지를 기다립니다.

```kotlin
consumer.receiveAsFlow().collect { message ->
    process(message.value)
    consumer.acknowledgeSuspend(message)
}
```

Flow는 Consumer를 소유하거나 닫지 않고 자동 ack도 하지 않습니다. collect를 다시 시작하면 같은 Consumer에서 새 receive loop를 만들므로 하나의 Consumer를 여러 collector가 공유할지 의도적으로 결정해야 합니다.

## 누적 ack의 제약

`acknowledgeCumulativeSuspend(message)`는 해당 메시지까지 누적 확인합니다. Exclusive 또는 Failover subscription에 사용합니다. release test는 Shared subscription에서 `PulsarClientException`이 발생하는 경계를 확인합니다.

누적 ack는 여러 업무 처리 결과를 한 위치로 압축합니다. 앞 메시지 중 하나가 실패했는데 뒤 메시지까지 누적 ack하지 않도록 순차 처리와 failure policy를 맞춥니다.

## 취소와 오류

Flow가 await 중 취소되면 대기 future에 `cancel(true)`를 호출하고 cancellation을 다시 던집니다. receive나 ack 실패도 Pulsar exception으로 전파됩니다. helper는 nack, retry, redelivery delay 또는 dead-letter 처리를 자동으로 추가하지 않습니다.

Consumer close도 Flow 책임이 아닙니다. `withConsumer` 또는 명시적 owner가 닫아야 하며, 1.12.1 `withConsumer`는 취소 중 close 완료를 보장하지 않습니다.

## 운영 관측

subscription backlog, unacked messages, redelivery, receive latency, handler latency와 ack failure를 따로 관찰합니다. handler가 느린데 receiver queue만 키우면 메모리와 unacked 범위가 커질 수 있습니다.

## Source와 tests

- [`ConsumerSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerSupport.kt)
- [`ConsumerExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerExtensions.kt)
- [`ConsumerSupportTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/consumer/ConsumerSupportTest.kt)
- [`ConsumerExtensionsTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/consumer/ConsumerExtensionsTest.kt)
