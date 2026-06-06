# Issue 695 Event Telemetry Helpers

## Context

Issue #695 adds reusable event publish and consume telemetry helpers after the shared observability contract from #696.

## Decision

Implement the first slice in `infra/micrometer` as generic Observation helpers instead of changing Kafka, NATS, Pulsar, or Spring modules directly.

The helper records stable low-cardinality tags by default and requires explicit opt-in for high-cardinality identifiers. Correlation IDs are sanitized before they can be recorded as high-cardinality values.

## Outcome

- `event.publish` and `event.consume` wrappers now share operation, messaging, event type, correlation presence, batch count, outcome, and exception semantics.
- Cancellation is observable as `outcome=CANCELLED` but is not reported as an Observation error.
- README examples cover Spring application event consumption and Kafka-style publish instrumentation.
- PR review tightened factory APIs by using companion `invoke` with private data class constructors.
- README now includes a shared English sequence diagram asset for event telemetry.

## Verification

```bash
./gradlew :bluetape4k-micrometer:test --tests 'io.bluetape4k.micrometer.observation.events.EventTelemetryObservationSupportTest'
```

Result: PASS, 7 tests.

```bash
./gradlew :bluetape4k-micrometer:test
```

Result: PASS, 80 tests and 1 pending.

## Future Work

Broker-specific modules can adopt these helpers later without changing broker APIs wholesale. Keep payload bodies, raw headers, exception messages, PII, secrets, and temporary destinations out of default tags.
