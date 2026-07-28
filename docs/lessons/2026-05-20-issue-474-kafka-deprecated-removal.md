# 이슈 474 Kafka Deprecated API 제거

## 배경

Issue #474는 deprecated infra API의 breaking cleanup을 시작한다. PR A는 compile-error deprecation과
compatibility test 뒤에 이미 staging된 Kafka/Kafka4 deprecated surface를 제거한다.

## 결정

Compile-error로 막힌 compatibility API를 유지하지 않고 deprecated Kafka/Kafka4 JDK-backed codec과
registry entry를 제거한다. Repo-local caller가 compatibility test에만 남아 있음을 확인한 뒤 send 및
metric alias도 제거한다.

## 결과

`infra/kafka`와 `infra/kafka4`는 이제 canonical Fory/Kryo codec family,
`suspendSend`/`suspendSendDefault`, `getMetricValueOrNull`만 expose한다. README file, CHANGELOG,
deprecated API inventory는 active API surface와 일치한다.

## 검증

- `rg`로 kafka/kafka4 source, test, README file에서 제거한 symbol 및 `@Deprecated` declaration이
  없음을 확인.
- `./gradlew :bluetape4k-kafka:compileKotlin :bluetape4k-kafka:compileTestKotlin :bluetape4k-kafka4:compileKotlin :bluetape4k-kafka4:compileTestKotlin --no-configuration-cache` 통과.
- `./gradlew :bluetape4k-kafka:test :bluetape4k-kafka4:test --no-configuration-cache` 통과:
  kafka 263 tests, kafka4 274 tests, failures 0, errors 0.
- `git diff --check` 통과.

## 향후 가이드

이후 #474 PR에서는 먼저 compatibility test를 canonical API test로 교체한 뒤 deprecated API를 삭제한다.
Testcontainers나 embedded infrastructure를 쓰는 module은 affected module test를 serial로 다시 실행한다.
