---
title: Reader와 시작 위치
description: subscription 없는 Reader, MessageId 시작 위치, 단건 읽기와 유한 readAsFlow 동작을 설명합니다.
manualId: bluetape4k-pulsar
chapterId: readers-and-positions
---

# Reader와 시작 위치

## Consumer와 다른 용도

Reader는 subscription을 만들지 않고 topic의 특정 위치부터 메시지를 읽습니다. ack도 하지 않습니다. replay, 데이터 검사, migration과 점검 도구처럼 소비 상태를 subscription으로 관리하지 않을 때 사용합니다.

```kotlin
val reader = client.reader(Schema.STRING) {
    topic("persistent://public/default/orders")
    startMessageId(MessageId.earliest)
}
```

`reader` helper는 native builder setup 뒤 동기 `create()`를 호출합니다. 시작 위치를 빠뜨리거나 잘못 설정했을 때의 검증은 Pulsar Client가 담당합니다.

## 시작 위치의 의미

`MessageId.earliest`는 보관된 가장 이른 위치에서, `MessageId.latest`는 Reader를 만든 시점의 끝에서 시작합니다. 특정 `MessageId`를 저장해 replay checkpoint로 사용할 수도 있지만 topic partition, retention과 message deletion 정책을 함께 고려해야 합니다.

시작 위치가 같은 Reader를 다시 만들면 같은 메시지를 다시 읽을 수 있습니다. Reader 자체는 처리 완료 checkpoint를 저장하지 않습니다.

## 단건 읽기

`readNextSuspend()`는 `readNextAsync()`를 기다리고 다음 `Message<T>`를 반환합니다.

```kotlin
val message = reader.readNextSuspend()
inspect(message.value)
```

메시지가 아직 없으면 다음 메시지를 기다릴 수 있습니다. 호출부 timeout과 cancellation 정책을 정합니다.

## readAsFlow는 현재 backlog를 drain한다

`readAsFlow()`는 `hasMessageAvailable()`이 true인 동안만 `readNextAsync()` 결과를 emit합니다.

```kotlin
client.withReader(Schema.STRING, {
    topic(topic)
    startMessageId(MessageId.earliest)
}) {
    readAsFlow().collect { inspect(it.value) }
}
```

`hasMessageAvailable()`이 false가 되는 순간 Flow는 정상 종료합니다. 새 메시지를 계속 기다리는 tailing stream이 아닙니다. release test도 미리 발행한 메시지는 읽고 latest 위치의 빈 topic에서는 빈 list를 반환하는 동작을 확인합니다.

검사 시점 직후 도착한 메시지는 이번 collect에 포함되지 않을 수 있습니다. 지속 스트림이 필요하면 Consumer의 `receiveAsFlow()`를 사용하거나 polling 범위를 명시적으로 설계합니다.

## 취소와 소유권

`readAsFlow`는 read future를 기다리는 중 취소되면 `future.cancel(true)`를 호출하고 cancellation을 다시 던집니다. Flow는 Reader를 닫지 않습니다. `withReader` 또는 호출자가 Reader와 parent client를 닫습니다.

1.12.1의 `withReader` close는 취소 불가능한 cleanup이 아니므로 long-running tool은 shutdown timeout과 close 결과를 직접 관리하는 편이 안전합니다.

## Source와 tests

- [`ReaderSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderSupport.kt)
- [`ReaderExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderExtensions.kt)
- [`ReaderSupportTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/reader/ReaderSupportTest.kt)
- [`ReaderExtensionsTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/reader/ReaderExtensionsTest.kt)
