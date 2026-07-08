# Lessons Learned — Issue #800 DefaultKafkaExporter failures (2026-07-09)

## Context

`DefaultKafkaExporter` reports Kafka producer send failures through an `ExportExceptionHandler`. The old synchronous failure path caught `Throwable`, reported only timeout/buffer exceptions, and silently returned `false` for other failures.

## Lesson

Exporter boundaries should catch non-fatal `Exception`, not `Throwable`. Fatal `Error` values must propagate, while every synchronous non-fatal send failure should use the same handler path as callback failures.

## Outcome

- Added regression coverage for generic synchronous `Exception` handling.
- Added regression coverage for fatal `Error` propagation.
- Preserved callback exception handling and timeout handling.

## Verification

- RED targeted test: 5 tests completed, 2 expected failures.
- GREEN targeted test: 5 passing.
- Module test: `:bluetape4k-kafka-logback:test`, 24 passing.
