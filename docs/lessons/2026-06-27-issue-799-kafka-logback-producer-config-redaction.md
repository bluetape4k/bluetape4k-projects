# Issue #799: Kafka Logback Producer Config Redaction

## Context

`bluetape4k-kafka-logback` accepted arbitrary Kafka producer configuration and
emitted Logback status messages for added config entries, malformed config
strings, and producer creation failures. Raw status messages can be collected by
operations tooling, so values from keys such as `sasl.jaas.config` and
`*.password` must be treated as sensitive even when they are not ordinary
application logs.

## Decision

Route all Kafka producer config status formatting through one internal helper:

- redact credential-bearing values by sensitive key fragments;
- keep non-sensitive keys and values visible for diagnostics;
- report malformed `key=value` input by payload length only;
- reuse the same formatter for add-config, producer creation success, and
  producer creation failure status messages.

## Verification

- RED: new `KafkaAppenderTest` redaction tests failed before the fix because
  added producer config and malformed config status messages contained raw
  secret values.
- GREEN: `./gradlew :bluetape4k-kafka-logback:test --tests "io.bluetape4k.kafka.logback.KafkaAppenderTest" --no-build-cache --no-daemon --no-configuration-cache`
  passed with 11 tests.
- `./gradlew :bluetape4k-kafka-logback:test --tests "*KafkaAppender*" --no-build-cache --no-daemon --no-configuration-cache`
  passed with 12 tests, including the Testcontainers-backed `KafkaAppenderIT`.
- `./gradlew :bluetape4k-kafka-logback:test --no-build-cache --no-daemon --no-configuration-cache`
  passed with 22 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew :bluetape4k-kafka-logback:compileTestKotlin --warning-mode all --no-daemon --no-configuration-cache`
  passed. Remaining warnings are existing Gradle Kotlin DSL deprecations outside
  the touched source/test code.
- Source scan found no remaining raw `$producerConfig`, `$keyValue`, or
  `value=$value` status formatting in `infra/kafka-logback`.
- `git diff --check` passed.

## Future Guidance

Status output is still an operational export surface. When logging dynamic
configuration or parse failures, redact by key before formatting and avoid
echoing the original malformed payload. Tests should assert both the absence of
secret substrings and the presence of useful non-sensitive diagnostics.

## Concurrency Helper Gate

No shared mutable state, coroutine lifecycle, thread contention, virtual-thread,
or `StructuredTaskScope` behavior changed. `MultithreadingTester`,
`SuspendedJobTester`, and `StructuredTaskScopeTester` are not applicable to this
status-formatting redaction fix.
