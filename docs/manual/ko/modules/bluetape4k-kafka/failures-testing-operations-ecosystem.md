---
title: 실패, 테스트, 운영과 생태계
description: delivery ambiguity, offset·codec 실패, server-free와 Testcontainers 검증, Kafka 4와 Logback 연동 경계를 정리합니다.
manualId: bluetape4k-kafka
chapterId: failures-testing-operations-ecosystem
---

# 실패, 테스트, 운영과 생태계

## 실패를 한 종류로 묶지 않는다

Kafka 애플리케이션의 실패는 최소한 다음처럼 나눕니다.

- serialize·deserialize와 type allowlist 실패
- producer local timeout, broker reject와 delivery 결과 불명
- consumer poll/rebalance와 offset commit 실패
- coroutine cancellation과 application shutdown
- Streams processing, state store와 restore 실패

같은 retry를 일괄 적용하면 duplicate, poison-pill loop 또는 rebalance 악화를 만들 수 있습니다. 실패 종류마다 재시도 가능 여부, idempotency와 DLQ 경로를 정합니다.

## 발송 결과와 업무 원자성

`suspendSend` 성공은 broker acknowledgment를 받았다는 뜻이지 database transaction과 함께 commit됐다는 뜻이 아닙니다. database 변경과 event 발행을 묶어야 하면 outbox를 사용합니다. timeout·cancellation은 “발송 안 됨”이 아니라 “호출자가 결과를 모름”으로 분류합니다.

## consumer 처리와 offset

처리 전에 offset이 commit되면 장애 시 record를 잃을 수 있고, 처리 뒤 commit하면 재처리될 수 있습니다. 후자를 선택할 때 handler를 idempotent하게 만듭니다. exactly-once API도 외부 HTTP·database side effect까지 자동 transaction으로 묶지 않습니다.

## 빠른 unit test

다음 영역은 broker 없이 검증할 수 있습니다.

- `TopicPartitionSupportTest`: parsing과 입력 오류
- codec tests: byte/string round trip, allowlist, poison pill
- coroutine producer mock tests: callback 실패와 future cancellation
- listener adapter tests: Spring utility delegation
- `KStreamDslTest`: Streams parameter factory

```bash
./gradlew :bluetape4k-kafka:test \
  --tests "io.bluetape4k.kafka.TopicPartitionSupportTest" \
  --tests "io.bluetape4k.kafka.codec.*" \
  --tests "io.bluetape4k.kafka.streams.kstream.KStreamDslTest" \
  --no-configuration-cache
```

producer·consumer, Spring template와 실제 Flow test는 `KafkaServer.Launcher.kafka` Testcontainer를 사용합니다. 무거운 infra test는 다른 module과 동시에 돌리지 않습니다.

## 애플리케이션 검증 목록

1. 이전·신규 codec을 교차 배치해 저장된 record를 읽습니다.
2. retry와 timeout에서 duplicate 처리와 idempotency를 검증합니다.
3. rebalance 중 처리·commit 순서와 graceful shutdown을 검사합니다.
4. TLS/SASL, ACL과 credential rotation을 production과 같은 설정으로 테스트합니다.
5. Streams topology upgrade와 state restoration 시간을 측정합니다.
6. DLQ replay가 같은 poison pill loop를 만들지 확인합니다.

## 운영 지표

producer error/retry/request latency/buffer, consumer lag/rebalance/poll/commit, DLQ count와 codec reject, Streams task/state restore를 분리해 봅니다. topic과 group은 허용 목록으로 제한하고 record key, exception message와 payload를 metric label에 넣지 않습니다.

## 인접 artifact 선택

| 필요 | artifact | 경계 |
| --- | --- | --- |
| Kafka 3.9.x, Spring Kafka 3.x, Jackson 2 | `bluetape4k-kafka` | 이 매뉴얼 |
| Kafka 4.2.x, Spring Kafka 4.x, Jackson 3 | [`bluetape4k-kafka4`](../bluetape4k-kafka4.md) | 같은 package를 공유하므로 둘 중 하나만 선택 |
| Logback event를 Kafka로 export | [`bluetape4k-kafka-logback`](../bluetape4k-kafka-logback.md) | appender/exporter 전용, 일반 producer API 대체 아님 |
| coroutine·Flow 기본기 | [`bluetape4k-coroutines`](../bluetape4k-coroutines.md) | Kafka delivery 의미는 이 모듈이 설명 |
| Kafka Testcontainer 공통 기반 | [`bluetape4k-testcontainers`](../bluetape4k-testcontainers.md) | application fixture와 lifecycle은 caller가 구성 |

## 1.12.1과 개발 branch의 차이

release 이후 Kafka test에 module별 임시 directory 설정이 추가됐고, consumer template close는 non-`AutoCloseable` receiver를 경고하며 close 실패를 log하고 삼키도록 바뀌었습니다. 이 매뉴얼의 lifecycle과 failure 설명은 1.12.1 동작인 “조건부 close, 경고 없음, close exception 전파”를 기준으로 합니다.

## Source와 tests

- [`AbstractKafkaTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/AbstractKafkaTest.kt)
- [`ProducerSupportTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/coroutines/ProducerSupportTest.kt)
- [`SuspendKafkaConsumerTemplateTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/spring/core/SuspendKafkaConsumerTemplateTest.kt)
- [`KStreamDslTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/streams/kstream/KStreamDslTest.kt)
- [`kafka4/build.gradle.kts`](../../../../../infra/kafka4/build.gradle.kts)
- [`kafka-logback/build.gradle.kts`](../../../../../infra/kafka-logback/build.gradle.kts)
