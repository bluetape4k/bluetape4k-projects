# 이슈 #799: Kafka Logback producer config redaction

## 배경

`bluetape4k-kafka-logback`은 arbitrary Kafka producer configuration을 받았고,
추가된 config entry, malformed config string, producer creation failure에 대해
Logback status message를 출력했다. Raw status message는 운영 도구에 수집될 수
있으므로 `sasl.jaas.config`, `*.password` 같은 key의 값은 일반 application log가
아니어도 sensitive하게 다뤄야 한다.

## 결정

모든 Kafka producer config status formatting을 하나의 internal helper로 보낸다.

- sensitive key fragment로 credential-bearing value를 redaction한다.
- non-sensitive key와 value는 진단을 위해 그대로 보여준다.
- malformed `key=value` input은 payload 길이만 보고한다.
- add-config, producer creation success, producer creation failure status message에서 같은 formatter를 재사용한다.

## 검증

- RED: 새 `KafkaAppenderTest` redaction test는 수정 전 added producer config와 malformed config status message가 raw secret value를 포함해 실패했다.
- GREEN: `./gradlew :bluetape4k-kafka-logback:test --tests "io.bluetape4k.kafka.logback.KafkaAppenderTest" --no-build-cache --no-daemon --no-configuration-cache`는 11개 test로 통과했다.
- `./gradlew :bluetape4k-kafka-logback:test --tests "*KafkaAppender*" --no-build-cache --no-daemon --no-configuration-cache`는 Testcontainers 기반 `KafkaAppenderIT`를 포함해 12개 test로 통과했다.
- `./gradlew :bluetape4k-kafka-logback:test --no-build-cache --no-daemon --no-configuration-cache`는 22개 test, 0 failures, 0 errors, 0 skipped로 통과했다.
- `./gradlew :bluetape4k-kafka-logback:compileTestKotlin --warning-mode all --no-daemon --no-configuration-cache`가 통과했다. 남은 warning은 touched source/test code 밖의 기존 Gradle Kotlin DSL deprecation이다.
- Source scan에서 `infra/kafka-logback`에 raw `$producerConfig`, `$keyValue`, `value=$value` status formatting이 남아 있지 않았다.
- `git diff --check`가 통과했다.

## 향후 지침

Status output도 운영 export surface다. 동적 configuration이나 parse failure를 log할
때는 formatting 전에 key 기준으로 redaction하고 원본 malformed payload를 다시
출력하지 않는다. 테스트는 secret substring 부재와 유용한 non-sensitive diagnostic
존재를 모두 검증해야 한다.

## 동시성 helper gate

Shared mutable state, coroutine lifecycle, thread contention, virtual-thread,
`StructuredTaskScope` 동작은 변경되지 않았다. `MultithreadingTester`,
`SuspendedJobTester`, `StructuredTaskScopeTester`는 이 status-formatting redaction
수정에 적용되지 않는다.
