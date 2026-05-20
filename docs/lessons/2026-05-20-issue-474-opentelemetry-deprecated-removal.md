# Issue 474 OpenTelemetry Deprecated API Removal

## Context

Issue #474 tracks staged removal of deprecated `infra/` APIs after the compatibility window. This lane only covers the OpenTelemetry module and stays independent from the Kafka cleanup PR.

## Decision

Remove the deprecated OpenTelemetry aliases instead of keeping forwarding shims:

- `SpanBuilder.useSuspendSpan`
- `spanExportOf`
- `batchSpanProcess`

The canonical APIs remain `useSpanSuspending`, `spanExporterOf`, and `batchSpanProcessorOf`.

## Outcome

OpenTelemetry no longer exposes deprecated alias APIs in main sources, and README examples now show only canonical names.

## Verification

- `./gradlew :bluetape4k-opentelemetry:compileKotlin :bluetape4k-opentelemetry:compileTestKotlin --no-configuration-cache`
  passed.
- `./gradlew :bluetape4k-opentelemetry:test --no-configuration-cache` passed with 73 tests.
- Removed one existing `!!` in `FlowSpanSupportTest` after compile surfaced an unnecessary non-null assertion warning.

## Future Guidance

Keep future #474 lanes narrow and independent. The shared deprecated inventory may conflict across parallel cleanup PRs; resolve it by preserving each lane's completed rows and keeping remaining work visible.
