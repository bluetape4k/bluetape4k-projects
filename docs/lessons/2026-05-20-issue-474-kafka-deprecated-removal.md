# Issue 474 Kafka Deprecated Removal

## Context

Issue #474 starts the breaking cleanup for deprecated infra APIs. PR A removes
the Kafka/Kafka4 deprecated surface that was already staged behind compile-error
deprecations and compatibility tests.

## Decision

Remove the deprecated Kafka/Kafka4 JDK-backed codecs and registry entries rather
than keeping compile-error gated compatibility APIs. Remove send and metric
aliases after confirming repo-local callers only exercised compatibility tests.

## Outcome

`infra/kafka` and `infra/kafka4` now expose only the canonical Fory/Kryo codec
families, `suspendSend`/`suspendSendDefault`, and `getMetricValueOrNull`.
README files, CHANGELOG, and the deprecated API inventory now match the active
API surface.

## Verification

- `rg` found no removed symbols or `@Deprecated` declarations in kafka/kafka4
  source, tests, or README files.
- `./gradlew :bluetape4k-kafka:compileKotlin :bluetape4k-kafka:compileTestKotlin :bluetape4k-kafka4:compileKotlin :bluetape4k-kafka4:compileTestKotlin --no-configuration-cache`
  passed.
- `./gradlew :bluetape4k-kafka:test :bluetape4k-kafka4:test --no-configuration-cache`
  passed: kafka 263 tests, kafka4 274 tests, failures 0, errors 0.
- `git diff --check` passed.

## Future Guidance

For later #474 PRs, first replace compatibility tests with canonical API tests,
then delete the deprecated APIs and rerun the affected module tests serially if
they use Testcontainers or embedded infrastructure.
