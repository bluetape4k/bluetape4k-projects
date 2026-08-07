---
title: 실패, 테스트, 운영과 생태계 경계
description: 입력 검증, not-found 변환, Testcontainers 검증, 운영 지표와 Spring integration 책임을 정리합니다.
manualId: bluetape4k-nats
chapterId: failures-testing-operations-ecosystem
---

# 실패, 테스트, 운영과 생태계 경계

## 실패를 네 종류로 나누기

NATS 통합에서는 실패 원인을 한 종류의 messaging exception으로 합치지 않는 편이 낫습니다.

| 종류 | 예 | 처리 기준 |
| --- | --- | --- |
| 호출자 입력 | blank subject·stream·bucket, 음수 timeout | 즉시 `IllegalArgumentException`, 재시도하지 않음 |
| 일시적 통신 | disconnect, timeout, `IOException` | operation 의미와 idempotency를 확인한 뒤 제한적으로 재시도 |
| server 정책 | authorization, invalid stream config, storage limit | 설정이나 권한을 고치며 자동 재시도로 숨기지 않음 |
| 정상적인 부재 | 없는 stream·consumer·bucket | `get*OrNull`, `exists*`, `forcedDelete*`가 일부 경로만 변환 |

`JET_STREAM_NOT_FOUND`가 아닌 JetStream 오류는 management helper가 그대로 던집니다. not-found처럼 보이는 사용자 업무 데이터 부재와 infrastructure resource 부재도 섞지 않습니다.

## retry와 중복

publish나 request가 timeout됐다는 사실만으로 server가 message를 받지 않았다고 단정할 수 없습니다. 응답을 받기 전에 network가 끊겼을 수 있습니다. retry가 가능한 command에는 업무 idempotency key를 넣고 consumer가 중복을 처리하도록 합니다.

JetStream redelivery는 정상 동작입니다. handler가 외부 database를 변경한 뒤 ack 전에 실패하면 같은 message가 다시 올 수 있습니다. ack 순서와 side effect를 application transaction 또는 inbox/outbox pattern으로 설계합니다.

## unit test와 server test

`ConnectionExtensionsTest`, option test와 service builder test는 mock으로 위임과 입력 검증을 확인합니다. 실제 subject routing, dispatcher, request-reply, publish ack와 storage는 NATS server 없이는 증명할 수 없습니다.

```bash
./gradlew :bluetape4k-nats:test --no-configuration-cache
```

모듈의 `AbstractNatsTest`는 `NatsServer.Launcher.nats` Testcontainer를 공유합니다. 이 fixture는 published artifact에 포함되지 않습니다. 애플리케이션도 production option을 반영한 자체 fixture를 만들고 다른 Testcontainers suite와 순차 실행합니다.

## 장애 시나리오

happy path 외에 다음을 검증합니다.

- connection을 끊었다가 server가 돌아왔을 때 reconnect와 subscription이 어떻게 회복되는지 확인합니다.
- responder가 없을 때 동기·비동기·suspend request 결과를 각각 확인합니다.
- JetStream handler가 ack 전에 실패했을 때 redelivery와 최대 delivery 정책을 확인합니다.
- slow consumer에서 pending limit과 error listener가 관찰되는지 확인합니다.
- stream purge·replace와 bucket delete가 승인된 환경에서만 실행되는지 확인합니다.

## 운영 지표와 log

core connection에는 connection event, reconnect, async error, slow consumer, request latency와 timeout을 기록합니다. subject 전체를 metric label로 쓰면 cardinality가 커질 수 있으므로 제한된 업무 category로 정규화합니다.

JetStream에는 stream message·byte 수, publish ack latency, consumer pending·redelivery·ack floor와 processing latency를 추가합니다. handler exception과 nak, term, timeout을 구분해야 재처리 loop를 찾을 수 있습니다.

credential, token, 전체 message payload는 기본 log에 남기지 않습니다. trace correlation이 필요하면 제한된 header와 message ID를 사용하고 개인정보와 header cardinality를 검토합니다.

## Spring과 다른 messaging 모듈의 경계

`bluetape4k-nats`는 `nats-spring` API만 `compileOnly`로 참조할 수 있게 합니다. Spring Boot auto-configuration, property binding, actuator health와 bean lifecycle은 제공하지 않습니다. Spring application이 이 기능을 원하면 직접 구성하고 coroutine dependency도 runtime에 추가합니다.

Kafka·Pulsar처럼 partition log와 consumer group ecosystem이 필요한 workload를 NATS KeyValue나 core queue subscription으로 억지로 바꾸지 않습니다. 반대로 간단한 subject routing과 request-reply만 필요한데 무거운 log platform을 기본 선택할 필요도 없습니다. 보존, replay, ordering, throughput과 운영 도구 요구를 기준으로 선택합니다.

## 1.12.1 검증 범위

1.12.1 production source에는 serializer abstraction, schema registry, retry policy, tracing hook, Spring auto-configuration이 없습니다. README example의 Jackson과 compressor는 test payload를 위한 dependency이며 이 module의 runtime serialization contract가 아닙니다.

## Source와 tests

- [`build.gradle.kts`](../../../../../infra/nats/build.gradle.kts)
- [`JetStreamApiException.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStreamApiException.kt)
- [`JetStreamManagement.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStreamManagement.kt)
- [`NatsManagementExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/NatsManagementExtensionsTest.kt)
- [`ConnectionExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/ConnectionExtensionsTest.kt)
- [`SimplePublishExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/SimplePublishExample.kt)
- [`RecreateConsumerExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/RecreateConsumerExample.kt)

문서 검증은 source link와 contract를 확인합니다. NATS server를 띄우는 전체 Testcontainers suite는 문서 변경만으로 반복 실행할 필요가 없으며, parent workflow에서 정한 통합 검증 단계에 맞춰 순차 실행합니다.
