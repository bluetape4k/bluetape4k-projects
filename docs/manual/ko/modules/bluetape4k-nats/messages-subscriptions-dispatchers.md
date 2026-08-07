---
title: Message, subscription과 dispatcher
description: NatsMessage DSL, pull-style Subscription, callback Dispatcher, unsubscribe와 drain 순서를 설명합니다.
manualId: bluetape4k-nats
chapterId: messages-subscriptions-dispatchers
---

# Message, subscription과 dispatcher

## message를 직접 만들 때

`natsMessageOf`는 subject, payload, reply-to와 header를 jNATS `NatsMessage`로 조립합니다. subject가 blank면 실패하며, payload가 `null`인 메시지도 허용합니다.

```kotlin
val message = natsMessageOf(
    subject = "documents.convert",
    data = requestBytes,
    replyTo = "documents.convert.reply",
    headers = Headers().apply {
        add("Content-Type", "application/json")
    },
)

connection.publish(message)
```

builder는 header 중복, payload schema와 serializer를 강제하지 않습니다. 같은 subject를 사용하는 publisher와 consumer가 encoding, schema evolution과 최대 크기를 별도 계약으로 합의해야 합니다.

## blocking subscription

`Connection.subscribe(subject)`가 반환한 `Subscription`에서는 `nextMessage(timeout)`으로 다음 메시지를 기다릴 수 있습니다. Kotlin extension은 음수 timeout을 거부하고, 제한 시간 안에 메시지가 없으면 `null`을 반환합니다.

```kotlin
val subscription = connection.subscribe("audit.created")
while (running) {
    val message = subscription.nextMessage(500.milliseconds) ?: continue
    handle(message)
}
subscription.unsubscribe()
```

이 호출은 현재 thread를 기다리게 합니다. coroutine dispatcher에서 무심코 반복하면 worker thread를 점유할 수 있습니다. callback dispatcher를 사용하거나 blocking loop를 명시적인 IO execution context에 둡니다.

## dispatcher callback

`createDispatcher`는 jNATS가 callback을 호출하는 subscription을 만듭니다.

```kotlin
val dispatcher = connection.createDispatcher()
dispatcher.subscribe("orders.*") { message ->
    handleOrderEvent(message)
}
```

handler 안에서 오래 blocking하면 같은 dispatcher의 다음 message가 밀릴 수 있습니다. 실제 thread와 serialization 정책은 jNATS dispatcher 설정을 확인합니다. callback에서 coroutine을 launch한다면 scope의 소유자, 동시성 제한과 shutdown join을 함께 설계합니다.

## unsubscribe와 drain

새 메시지를 더 받지 않으려면 subscription을 unsubscribe하거나 dispatcher subject를 해제합니다. 이미 callback이 시작한 작업은 unsubscribe만으로 취소되지 않습니다. 종료 순서는 보통 다음처럼 구성합니다.

1. readiness를 내려 새 외부 요청을 막습니다.
2. subscription 또는 dispatcher의 신규 전달을 중지합니다.
3. 이미 시작한 handler 작업을 기다립니다.
4. consumer와 connection을 drain합니다.
5. connection을 닫습니다.

`Consumer.drainSuspending`은 jNATS consumer drain future를 기다리지만 handler가 만든 임의의 child job까지 추적하지는 않습니다.

## slow consumer와 backpressure

core dispatcher는 application 처리 속도보다 publish 속도가 빠르면 pending message와 memory pressure가 생깁니다. callback에서 무제한 coroutine을 만들면 jNATS queue의 압력을 application heap과 downstream으로 옮길 뿐입니다.

최대 동시 처리 수, pending limit, drop 또는 disconnect 정책을 정하고 slow-consumer event를 metric으로 기록합니다. 반드시 처리해야 하는 message라면 core dispatcher 대신 JetStream consumer의 ack와 redelivery를 사용합니다.

## Source와 tests

- [`NatsMessage.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/NatsMessage.kt)
- [`SubscriptionExtensions.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/SubscriptionExtensions.kt)
- [`Consumer.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/Consumer.kt)
- [`NatsMessageTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/NatsMessageTest.kt)
- [`SubscriptionExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/SubscriptionExtensionsTest.kt)
- [`SimplePublishExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/SimplePublishExample.kt)

1.12.1 helper는 `Flow<Message>` adapter나 bounded worker pool을 제공하지 않습니다. application이 처리 모델과 backpressure를 선택합니다.
