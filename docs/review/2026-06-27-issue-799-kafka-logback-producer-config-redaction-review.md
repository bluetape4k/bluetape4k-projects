# Local 검토 - Issue #799 Kafka Logback Producer Config Redaction

Date: 2026-06-27
Scope: `infra/kafka-logback`
Issue: #799

## Verdict

No P0/P1 findings found in the current diff.

## Reviewed Risks

- Security/trust boundary: producer config values are redacted before Logback
  status output on add-config, malformed config, producer creation success, and
  producer creation failure paths.
- Diagnostic quality: non-sensitive entries such as `client.id` remain visible,
  while malformed input reports payload length without echoing the original
  payload.
- Compatibility: public appender APIs and Kafka producer construction semantics
  are unchanged; only status-message formatting and stricter malformed string
  parsing changed.
- Test helper gate: concurrency helpers are intentionally not used because this
  diff does not add shared state, coroutine behavior, virtual threads, or
  structured task scope behavior.

## 검증 Evidence

- TDD red: `KafkaAppenderTest` redaction tests failed before the fix because
  status messages contained `kafka-jaas-secret`, `truststore-secret`, and raw
  malformed payload text.
- `./gradlew :bluetape4k-kafka-logback:test --tests "io.bluetape4k.kafka.logback.KafkaAppenderTest" --no-build-cache --no-daemon --no-configuration-cache`:
  PASS, 11 tests.
- `./gradlew :bluetape4k-kafka-logback:test --tests "*KafkaAppender*" --no-build-cache --no-daemon --no-configuration-cache`:
  PASS, 12 tests including `KafkaAppenderIT`.
- `./gradlew :bluetape4k-kafka-logback:test --no-build-cache --no-daemon --no-configuration-cache`:
  PASS, 22 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew :bluetape4k-kafka-logback:compileTestKotlin --warning-mode all --no-daemon --no-configuration-cache`:
  PASS. Remaining warnings are existing Gradle Kotlin DSL deprecations outside
  touched source/test code.
- `rg -n '\$producerConfig|\$keyValue|value=\$value|Add producer config|Fail to add producer config value|Create Kafka Producer|Fail to create Kafka Producer|KafkaProducerConfigDiagnostics' infra/kafka-logback/src/main/kotlin infra/kafka-logback/src/test/kotlin`:
  PASS; remaining status messages route through `KafkaProducerConfigDiagnostics`.
- CodeGraph review context against `develop`: low risk, 0 impacted nodes for
  the changed files.
- `git diff --check`: PASS.

## Residual Risk

Sensitive key detection is intentionally conservative and based on lowercased
key fragments. Future Kafka config keys that carry secrets should be added to
`KafkaProducerConfigDiagnostics` with matching redaction tests.
