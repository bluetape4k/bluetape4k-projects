# Issue #800 Review — DefaultKafkaExporter send failure handling (2026-07-09)

## Scope

- Module: `infra/kafka-logback`
- Issue: #800 `P1: Report all non-fatal DefaultKafkaExporter send failures`
- Files:
  - `infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/DefaultKafkaExporter.kt`
  - `infra/kafka-logback/src/test/kotlin/io/bluetape4k/kafka/logback/exporter/DefaultKafkaExporterTest.kt`

## Finding

`DefaultKafkaExporter.export` caught `Throwable`, called the export exception handler only for `BufferExhaustedException` and `TimeoutException`, and returned `false` for all other synchronous send failures. This hid ordinary non-fatal producer failures and also swallowed fatal `Error` values.

## Fix Review

- Changed the synchronous send failure boundary from `Throwable` to `Exception`.
- All synchronous non-fatal `Exception` failures now call `exceptionHandler.handle(event, e)` and return `false`.
- Fatal `Error` values are no longer caught and propagate to the caller.
- Callback exception handling remains unchanged.
- Public KDoc now states the non-fatal `Exception` and fatal `Error` contract in English.

## TDD Evidence

- Baseline targeted test before changes:
  - `repo-test-summary -- ./gradlew :bluetape4k-kafka-logback:test --tests "io.bluetape4k.kafka.logback.exporter.DefaultKafkaExporterTest"`
  - Result: PASS, 3 passing.
- RED after adding regression tests:
  - Same command.
  - Result: FAIL, 5 tests completed, 2 failed.
  - Expected failures:
    - Generic `IllegalStateException` did not call `exceptionHandler.handle(...)`.
    - `OutOfMemoryError` was swallowed instead of propagated.
- GREEN targeted test:
  - Same command.
  - Result: PASS, 5 passing.
- Module verification:
  - `repo-test-summary -- ./gradlew :bluetape4k-kafka-logback:test`
  - Result: PASS, 24 passing.
- Whitespace verification:
  - `git diff --check`
  - Result: PASS.
- IDE diagnostics:
  - IntelliJ diagnostics were not available in this session; the exposed LSP diagnostics transport was closed.
  - Fallback evidence is Gradle `compileKotlin` through module test plus `git diff --check`.

## Review Verdict

- Local 7-tier equivalent review: PASS
- Impact evidence: CodeGraph reports `DefaultKafkaExporter` affects the exporter, `KafkaAppender`, and kafka-logback tests; no cross-module production callers beyond the appender path require a wider API change.
- P0/P1 findings: 0
- P2/P3 findings: 0
- Remaining risk: low; behavior change is limited to synchronous `Producer.send` exception handling and covered by direct regression tests.

## 7-Tier Notes

| Tier | Result | Evidence |
|---|---|---|
| Correctness/API | PASS | `DefaultKafkaExporter.kt:34-36` catches `Exception`, reports it, and returns `false`; fatal `Error` is outside the catch boundary. |
| Stability/lifecycle | PASS | Callback exception handling remains at `DefaultKafkaExporter.kt:28-31`; synchronous and callback paths both use `exceptionHandler.handle(...)`. |
| Tests/evidence | PASS | `DefaultKafkaExporterTest.kt:87-108` covers generic `Exception` handling and fatal `Error` propagation; targeted and module tests pass. |
| Security | PASS | No new inputs, serialization, networking credentials, or logging of sensitive data. |
| Performance | PASS | Handler call is only on exceptional synchronous send failure; no hot-path allocation or blocking added. |
| Maintainability | PASS | Removes special-case imports and documents one explicit exception boundary. |
| Docs/KDoc | PASS | Public KDoc is English and describes callback, non-fatal, and fatal behavior. |
