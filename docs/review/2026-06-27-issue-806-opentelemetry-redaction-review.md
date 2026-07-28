# Local 검토 - Issue 806 OpenTelemetry Redacted Exception Telemetry

Date: 2026-06-27
Scope: `infra/opentelemetry`
Issue: #806

## Verdict

No P0/P1 findings found in the current diff.

## Reviewed Risks

- Security/trust boundary: helper-managed OpenTelemetry failure paths no longer export raw exception messages through span status descriptions or `exception.message` event attributes.
- Coroutine/Flow lifecycle: `CancellationException` branches still rethrow before failure recording, preserving `UNSET` status and structured cancellation behavior.
- Public API compatibility: existing helper signatures are unchanged. The behavior change is a safer default for exported telemetry.
- Test helper gate: `SuspendedJobTester` is used for coroutine and Flow redaction stress coverage. `MultithreadingTester` and `StructuredTaskScopeTester` are intentionally not used because the fix does not introduce shared production state, thread contention, virtual-thread behavior, or structured task scope behavior.

## 검증 Evidence

- TDD red: targeted redaction tests failed before the fix because exported telemetry still contained secret-bearing exception messages.
- `./gradlew :bluetape4k-opentelemetry:compileTestKotlin --warning-mode all --no-daemon --no-configuration-cache`: PASS. Existing Gradle Kotlin DSL delegated-property deprecation warning remains outside touched source/test code.
- Targeted redaction tests, including `SuspendedJobTester` stress coverage: PASS.
- `./gradlew :bluetape4k-opentelemetry:test --rerun-tasks :bluetape4k-opentelemetry:check --no-daemon --no-configuration-cache`: PASS, 78 tests, 0 failures, 0 skipped.
- `git diff --check`: PASS.
- CodeGraph `get_affected_flows` against `develop`: 0 affected registered flows for 8 tracked changed files.

## Residual Risk

Callers that explicitly call raw OpenTelemetry `recordException(error)` can still export raw exception messages. README guidance documents that as opt-in behavior, not helper default behavior.
