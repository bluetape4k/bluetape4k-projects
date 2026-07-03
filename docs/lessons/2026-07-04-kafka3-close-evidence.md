# Kafka receiver close evidence should stay visible

## Context

Issue #950 found that Kafka3 suspend consumer shutdown did not expose receiver
close failures or non-`AutoCloseable` receiver evidence, while Kafka4 already
logged both paths.

## Decision

Port the Kafka4 close pattern to Kafka3:

- Warn when `KafkaReceiver` does not implement `AutoCloseable`.
- Wrap receiver close with `runCatching` and log failures instead of throwing
  them from `close()`/`destroy()`.

## Verification

- `./gradlew :bluetape4k-kafka:test --tests 'io.bluetape4k.kafka.spring.core.SuspendKafkaConsumerTemplateTest'`
- `git diff --check`

## Future guidance

Kafka3 and Kafka4 suspend templates should keep lifecycle diagnostics in parity.
Resource close paths should preserve shutdown progress while logging close
failure evidence with the receiver class.
