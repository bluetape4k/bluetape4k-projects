---
title: Core pub-sub와 request-reply
description: core NATS의 휘발성 전달, publish·flush, 동기·비동기·suspend request와 timeout 경계를 설명합니다.
manualId: bluetape4k-nats
chapterId: core-pubsub-request-reply
---

# Core pub-sub와 request-reply

## core NATS의 전달 모델

core NATS는 subject에 발행한 메시지를 현재 관심을 표시한 subscriber에게 전달합니다. stream에 저장하는 단계가 없으므로 subscriber가 없던 동안의 메시지를 나중에 replay하지 않습니다. 짧은 notification, cache invalidation처럼 최신 event만 의미가 있는 흐름에 잘 맞습니다.

```kotlin
connection.publish("catalog.changed", "A-100")
connection.flush(2.seconds)
```

문자열 overload는 body를 UTF-8 byte array로 바꾸고 jNATS `publish`에 위임합니다. blank subject는 거부하지만 payload schema, content type과 크기는 검사하지 않습니다.

## `flush`의 의미

`flush(Duration)`은 Kotlin `Duration`을 Java `Duration`으로 바꿉니다. server와 round trip을 완료하므로 이전 publish command가 server까지 전달됐는지 확인할 때 유용합니다. 다만 subscriber 처리 완료, database 반영, JetStream 저장을 보장하지 않습니다.

subscriber가 처리했다는 확인이 필요하면 request-reply나 업무 수준 acknowledgment subject를 설계합니다. 저장 acknowledgment가 필요하면 `JetStream.publish`의 `PublishAck`를 사용합니다.

## request-reply

request는 임시 inbox를 만들고 지정한 subject의 responder가 보낸 첫 응답을 기다리는 jNATS pattern입니다.

```kotlin
val response = connection.request(
    subject = "inventory.reserve",
    body = "{\"sku\":\"A-100\",\"qty\":1}",
    timeout = 500.milliseconds,
)

if (response == null) {
    // timeout을 업무 실패로 변환할지 호출부에서 결정
}
```

동기 overload는 `Message?`를 반환합니다. timeout이나 responder 부재를 `null`과 예외 중 무엇으로 표현하는지는 선택한 jNATS overload에 따라 다르므로 wrapper 이름만 보고 단정하지 않습니다.

## 비동기와 coroutine

`requestAsync`는 `CompletableFuture<Message>`를 그대로 반환합니다. timeout이 있으면 jNATS `requestWithTimeout`, 없으면 일반 async request를 호출합니다. `requestSuspending`과 `requestWithTimeoutSuspending`은 future를 `await()`합니다.

```kotlin
val response = withTimeout(1.seconds) {
    connection.requestWithTimeoutSuspending(
        "inventory.reserve",
        payload,
        timeout = 800.milliseconds,
    )
}
```

jNATS timeout과 coroutine `withTimeout`을 함께 쓰면 두 제한 시간이 경쟁할 수 있습니다. 한 층을 운영 timeout의 기준으로 정하고, exception log와 metric에서 어느 제한이 먼저 끝났는지 구분합니다.

coroutine 취소는 `await()`에서 전파됩니다. 취소가 server의 handler 실행까지 중단시킨다고 가정하면 안 됩니다. 이미 발행된 request의 외부 side effect는 계속 진행될 수 있으므로 idempotency key나 업무 상태 조회가 필요할 수 있습니다.

## queue group responder

여러 instance가 같은 service subject를 처리할 때 queue subscription으로 한 instance만 요청을 받게 할 수 있습니다. 이는 load distribution이지 durable queue가 아닙니다. 모든 responder가 내려간 동안 request를 보관해야 한다면 JetStream consumer나 별도 workflow가 필요합니다.

## Source와 examples

- [`ConnectionExtensions.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/ConnectionExtensions.kt)
- [`ConnectionExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/ConnectionExtensionsTest.kt)
- [`PubSubExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/PubSubExample.kt)
- [`RequestReplyExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/RequestReplyExample.kt)
- [`CoreReplyRequestPatterns.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/CoreReplyRequestPatterns.kt)

`RequestReplyExample`은 dispatcher responder와 동기·비동기 request를 함께 보여 줍니다. no-responder와 timeout의 구체적인 exception 형태는 사용하는 jNATS API와 server 설정으로 통합 테스트합니다.
