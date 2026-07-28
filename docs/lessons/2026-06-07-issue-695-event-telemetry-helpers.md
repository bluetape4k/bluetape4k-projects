# 이슈 695 Event telemetry helper

## 배경

issue #695는 #696의 shared observability contract 이후 reusable event publish/consume
telemetry helper를 추가한다.

## 결정

Kafka, NATS, Pulsar, Spring module을 직접 바꾸지 않고 첫 slice를 `infra/micrometer`의
generic Observation helper로 구현한다.

helper는 기본적으로 stable low-cardinality tag를 기록하고, high-cardinality identifier는
명시적인 opt-in을 요구한다. correlation ID는 high-cardinality value로 기록되기 전에
sanitize된다.

## 결과

- `event.publish`와 `event.consume` wrapper는 이제 operation, messaging, event type,
  correlation presence, batch count, outcome, exception semantic을 공유한다.
- cancellation은 `outcome=CANCELLED`로 관찰 가능하지만 Observation error로 보고하지 않는다.
- README example은 Spring application event consumption과 Kafka-style publish
  instrumentation을 다룬다.
- PR review는 private data class constructor와 companion `invoke`를 사용해 factory API를
  좁혔다.
- README에는 event telemetry용 shared English sequence diagram asset이 포함됐다.

## 검증

```bash
./gradlew :bluetape4k-micrometer:test --tests 'io.bluetape4k.micrometer.observation.events.EventTelemetryObservationSupportTest'
```

결과: PASS, 7 tests.

```bash
./gradlew :bluetape4k-micrometer:test
```

결과: PASS, 80 tests and 1 pending.

## 향후 작업

broker-specific module은 나중에 broker API 전체를 바꾸지 않고 이 helper를 채택할 수
있다. payload body, raw header, exception message, PII, secret, temporary destination은
default tag에서 제외한다.
