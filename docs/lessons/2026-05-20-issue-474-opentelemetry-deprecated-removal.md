# 이슈 474 OpenTelemetry Deprecated API 제거

## 배경

Issue #474는 compatibility window 이후 deprecated `infra/` API를 단계적으로 제거한다. 이 lane은
OpenTelemetry module만 다루며 Kafka cleanup PR과 독립적으로 유지한다.

## 결정

Forwarding shim을 유지하지 않고 deprecated OpenTelemetry alias를 제거한다:

- `SpanBuilder.useSuspendSpan`
- `spanExportOf`
- `batchSpanProcess`

Canonical API는 `useSpanSuspending`, `spanExporterOf`, `batchSpanProcessorOf`로 유지한다.

## 결과

OpenTelemetry main source는 더 이상 deprecated alias API를 expose하지 않고, README example도
canonical name만 보여준다.

## 검증

- `./gradlew :bluetape4k-opentelemetry:compileKotlin :bluetape4k-opentelemetry:compileTestKotlin --no-configuration-cache` 통과.
- `./gradlew :bluetape4k-opentelemetry:test --no-configuration-cache` 73 tests로 통과.
- Compile이 unnecessary non-null assertion warning을 드러낸 뒤 `FlowSpanSupportTest`의 기존 `!!` 1개를 제거.

## 향후 가이드

미래 #474 lane은 좁고 독립적으로 유지한다. Shared deprecated inventory는 parallel cleanup PR 사이에서
conflict 날 수 있으므로 각 lane의 completed row를 보존하고 remaining work를 계속 보이게 둔다.
