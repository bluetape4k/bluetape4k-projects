---
title: 취소, 실패, 테스트와 운영
description: Future 취소와 server 경계, close 한계, Testcontainers 검증과 production 관측 항목을 정리합니다.
manualId: bluetape4k-pulsar
chapterId: cancellation-failures-testing-operations
---

# 취소, 실패, 테스트와 운영

## suspend와 Flow의 취소 범위

단건 helper는 Pulsar의 `CompletableFuture`를 `awaitSuspending()`으로 기다립니다. 반복 Flow인 `sendAsFlow`, `receiveAsFlow`, `readAsFlow`는 await 중 `CancellationException`을 잡아 현재 future에 `cancel(true)`를 호출한 뒤 취소를 다시 던집니다.

이 동작은 coroutine이 대기에서 빠져나오는 데 필요합니다. 하지만 broker가 이미 받아들인 send를 취소하거나 server-side receive·read 작업이 중단됐다는 증거는 아닙니다. 호출자는 발행 중복과 수신 redelivery를 정상적인 분산 시스템 경계로 다룹니다.

## close 실패와 취소

1.12.1 `withPulsarClient`, `withProducer`, `withConsumer`, `withReader`는 `finally`에서 `closeAsync()`를 기다리고 실패를 warning으로 기록합니다.

두 제한이 있습니다.

- close 실패를 호출자에게 다시 던지지 않으므로 block 성공만으로 정상 종료를 증명할 수 없습니다.
- close await를 `NonCancellable`로 감싸지 않아 이미 취소된 context에서 cleanup 완료를 보장하지 않습니다.

이 한계는 release source에 대한 설명입니다. 이후 branch에 추가된 `PulsarCloseSupport`와 cancellation cleanup test를 1.12.1 계약에 소급하지 않습니다.

## 실패 처리 원칙

Pulsar Client 예외를 그대로 받되 domain boundary에서 필요한 정보만 안정된 예외로 바꿉니다. timeout, authentication, authorization, schema rejection, producer queue full과 ack failure를 하나의 “메시징 실패”로 뭉개지 않습니다.

retry 전에는 작업이 멱등한지 확인합니다. send timeout은 broker 저장 여부가 불확실할 수 있고, handler 성공 뒤 ack failure는 redelivery를 만들 수 있습니다. 업무 key와 처리 기록으로 중복을 흡수합니다.

## release test가 증명하는 것

`AbstractPulsarTest`는 Testcontainers `PulsarContainer`를 시작합니다. release test는 다음을 확인합니다.

- URL과 setup-only client 생성
- producer·consumer·reader block helper의 정상 경로
- 단건, message DSL, Flow 발행과 수신
- 개별 ack, Exclusive 누적 ack, Shared 누적 ack 실패
- Jackson 2·3 encode/decode, clone과 broker round trip
- earliest Reader의 backlog 읽기와 latest Reader의 빈 결과

## 증명하지 않는 것

1.12.1 test에는 block이 취소되는 동안 close가 끝나는지 검증하는 fixture가 없습니다. broker restart, network partition, auth rotation, schema evolution, retry·dead-letter, backpressure 한계와 장기 soak도 다루지 않습니다.

전체 module test는 실제 container를 사용하므로 이 문서 작업에서는 실행하지 않습니다. 애플리케이션 CI에서는 다른 Testcontainers suite와 순차 실행하고, mapper round trip 같은 server-free test를 먼저 분리합니다.

## 운영 checklist

- client connection과 reconnect 횟수
- producer send latency, pending queue, timeout과 실패율
- consumer backlog, unacked, redelivery, handler와 ack latency
- reader start position, 처리 position과 lag
- schema rejection과 payload decode failure
- shutdown 시작·종료 시각, in-flight 수와 close failure

metric label에는 제한된 topic·subscription·result code만 사용합니다. message key, payload, exception message와 무제한 tenant ID를 label에 넣지 않습니다.

## Source와 tests

- [`PulsarClientSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/PulsarClientSupport.kt)
- [`ProducerExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerExtensions.kt)
- [`ConsumerExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerExtensions.kt)
- [`ReaderExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderExtensions.kt)
- [`AbstractPulsarTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/AbstractPulsarTest.kt)
