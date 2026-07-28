# Issue 695 Event Telemetry Helpers 검토

## Scope

- Issue: #695
- Module: `infra/micrometer`
- Change type: Fast Track feature
- Review target: reusable Observation helpers for event publish and consume paths

## 발견 사항

- P0: 0
- P1: 0
- P2: 0

## 증거

- Added `event.publish` and `event.consume` Observation wrappers under `io.bluetape4k.micrometer.observation.events`.
- Low-cardinality tags follow the #696 contract: operation, messaging system, destination, event type, correlation presence, bounded batch count, and outcome.
- High-cardinality identifiers are explicit opt-ins and use sanitized correlation ID handling.
- Non-cancellation errors call `Observation.error`, record bounded exception type, and rethrow.
- Cancellation records `outcome=CANCELLED`, rethrows, and does not call `Observation.error`.
- Documentation warns against payloads, raw headers, exception messages, PII, secrets, query strings, and temporary destinations.
- PR review feedback converted data class construction to private constructors with companion `invoke` factories.
- README event telemetry sequence diagram added as a shared SVG/PNG asset pair.

## Verification

```bash
./gradlew :bluetape4k-micrometer:test --tests 'io.bluetape4k.micrometer.observation.events.EventTelemetryObservationSupportTest'
```

Result: PASS, 7 tests.

```bash
./gradlew :bluetape4k-micrometer:test
```

Result: PASS, 80 tests and 1 pending.

```bash
git diff --check
```

Result: PASS.

CodeGraph impact radius:

- Risk: low
- Directly changed graph nodes: 0
- Impacted nodes within 2 hops: 0
- Additional affected files: 0

Diagram validation:

- Rendered `docs/images/readme-diagrams/infra-micrometer-sequence-03.svg` to matching PNG.
- Inspected the rendered PNG for readable labels, separated sequence routes, and note text inside its box.
- SVG font scan found no `Inter`, `Arial`, or `Helvetica` references.

## Gate

PASS. No P0 or P1 findings found in the local review.
