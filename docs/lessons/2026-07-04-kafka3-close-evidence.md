# Kafka receiver close evidence는 계속 보여야 한다

## 배경

이슈 #950은 Kafka4가 이미 두 경로를 모두 log하는 반면, Kafka3 suspend consumer
shutdown은 receiver close failure나 non-`AutoCloseable` receiver evidence를 노출하지
않는다는 점을 확인했다.

## 결정

Kafka4 close pattern을 Kafka3로 옮긴다.

- `KafkaReceiver`가 `AutoCloseable`을 구현하지 않으면 warn한다.
- Receiver close를 `runCatching`으로 감싸고, `close()`/`destroy()`에서 던지는 대신 failure를 log한다.

## 검증

- `./gradlew :bluetape4k-kafka:test --tests 'io.bluetape4k.kafka.spring.core.SuspendKafkaConsumerTemplateTest'`
- `git diff --check`

## 향후 지침

Kafka3와 Kafka4 suspend template은 lifecycle diagnostic parity를 유지해야 한다.
Resource close path는 shutdown progress를 보존하면서 receiver class와 함께 close
failure evidence를 log해야 한다.
