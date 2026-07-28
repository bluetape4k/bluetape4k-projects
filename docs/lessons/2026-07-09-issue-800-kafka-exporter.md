# 교훈: 이슈 #800 `DefaultKafkaExporter` failure (2026-07-09)

## 배경

`DefaultKafkaExporter`는 Kafka producer send failure를 `ExportExceptionHandler`로
보고한다. 기존 synchronous failure path는 `Throwable`을 잡고 timeout/buffer exception만
보고했으며, 다른 failure에는 조용히 `false`를 반환했다.

## 교훈

Exporter boundary는 `Throwable`이 아니라 non-fatal `Exception`을 잡아야 한다. Fatal
`Error`는 전파되어야 하며, 모든 synchronous non-fatal send failure는 callback failure와
같은 handler path를 사용해야 한다.

## 결과

- Added regression coverage for generic synchronous `Exception` handling.
- Added regression coverage for fatal `Error` propagation.
- Preserved callback exception handling and timeout handling.

## 검증

- RED targeted test: 5 tests completed, 2 expected failures.
- GREEN targeted test: 5 passing.
- Module test: `:bluetape4k-kafka-logback:test`, 24 passing.
