---
title: JetStream stream과 consumer
description: PublishAck, stream reconciliation, durable ConsumerContext, fetch 제한과 파괴적 관리 작업을 설명합니다.
manualId: bluetape4k-nats
chapterId: jetstream-streams-consumers
---

# JetStream stream과 consumer

## core NATS와 다른 점

JetStream은 subject message를 stream에 저장하고 consumer 상태를 관리합니다. publisher는 `PublishAck`로 server가 message를 stream에 받아들였는지 확인할 수 있고, durable consumer는 마지막 처리 위치를 이어갈 수 있습니다.

```kotlin
val jetStream = connection.jetStream()
val ack = jetStream.publishSuspending(
    subject = "orders.created",
    body = "{\"orderId\":\"O-100\"}",
)
logger.debug { "stream=${ack.stream} sequence=${ack.seq}" }
```

`publishSuspending`은 async publish future를 기다립니다. coroutine 취소나 future 실패는 호출자에게 전파됩니다. ack를 받았다는 사실은 consumer의 업무 처리가 끝났다는 뜻이 아닙니다.

## stream configuration

`streamConfiguration` DSL은 jNATS builder를 그대로 사용합니다.

```kotlin
val config = streamConfiguration("ORDERS") {
    subjects("orders.*")
    storageType(StorageType.File)
    replicas(3)
    maxAge(Duration.ofDays(7))
}

val info = connection.jetStreamManagement().addStream(config)
```

retention, discard policy, max bytes, replicas와 storage type은 데이터 보존과 비용을 바꿉니다. library default에 맡기지 말고 운영 요구로 관리합니다.

## create, update와 replace

`createStream`은 기본 storage가 `Memory`이고 전달한 subject로 새 stream을 만듭니다. `createStreamOrUpdateSubjects`는 stream이 없으면 만들고, 있으면 기존 subject 순서를 유지하면서 빠진 subject만 뒤에 추가합니다. 이미 모두 있으면 update call도 하지 않습니다.

반대로 `createOrReplaceStream`은 기존 stream을 삭제하고 다시 만듭니다. 저장된 message와 consumer 상태를 잃을 수 있으므로 일반적인 idempotent update로 오해하면 안 됩니다. production startup에서는 non-destructive update를 우선하고 replace는 명시적 migration이나 test fixture에 제한합니다.

## durable consumer

`consumerContextOf(connection, streamName, consumerName)`은 이름을 durable로 둔 consumer configuration을 만들고 `createOrUpdateConsumer`를 호출합니다.

```kotlin
val consumer = consumerContextOf(
    connection,
    streamName = "ORDERS",
    consumerName = "billing",
)

val message = consumer.next()
try {
    bill(message)
    message.ack()
} catch (e: Exception) {
    message.nak()
    throw e
}
```

ack policy, deliver policy, filter subject, max delivery와 ack wait는 업무 재처리 계약입니다. 단순 factory overload는 durable 이름만 지정하므로 나머지 값이 중요하면 `ConsumerConfiguration`을 직접 만들어 두 번째 overload로 전달합니다.

## bounded fetch

`fetchConsumeOptionsOf`의 기본은 최대 100 messages, 만료 1,000ms이며 선택적으로 max bytes를 지정합니다. builder가 실제 값 검증을 담당합니다.

```kotlin
val fetchOptions = fetchConsumeOptionsOf(
    maxMessages = 50,
    expiresInMillis = 2_000,
    maxBytes = 4L * 1024 * 1024,
)
```

message 수와 byte 수를 함께 제한해야 큰 payload 하나가 batch memory를 독점하는 상황을 줄일 수 있습니다. 처리 실패 시 ack 여부, partial batch와 재전달 순서를 test합니다.

## not-found 처리

`getStreamInfoOrNull`, `streamExists`, `getConsumerInfoOrNull`, `consumerExists`는 JetStream not-found만 `null` 또는 `false`로 바꿉니다. permission, invalid configuration, timeout과 network 실패는 그대로 전파합니다.

`forcedPurgeStream`과 `forcedDelete*`도 동일합니다. 대상이 이미 없을 때만 정상으로 끝나며, 실제 purge·delete가 안전한지는 호출자가 판단해야 합니다.

## Source와 tests

- [`JetStream.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStream.kt)
- [`JetStreamManagement.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStreamManagement.kt)
- [`ConsumerContext.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/ConsumerContext.kt)
- [`FetchConsumeOptions.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/api/FetchConsumeOptions.kt)
- [`NatsManagementExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/NatsManagementExtensionsTest.kt)
- [`ContextExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/jetstream/simple/ContextExample.kt)
- [`FetchMessagesExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/jetstream/simple/FetchMessagesExample.kt)

test는 subject 병합 순서와 불필요한 update 생략, not-found 이외 오류 전파를 확인합니다. retention과 redelivery는 실제 server를 둔 통합 테스트로 보완합니다.
