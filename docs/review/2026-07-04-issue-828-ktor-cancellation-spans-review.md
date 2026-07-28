# Issue 828 Ktor Cancellation Span 검토

## Scope

- Issue: #828 `P1: Keep Ktor cancellation spans out of ERROR status`
- Milestone: 1.11.1
- Branch: `fix/issue-828-ktor-cancellation-spans`
- Target: `ktor/observability`

## 7-Tier 검토

| Tier | Result | Evidence |
|---|---|---|
| P0 Correctness | PASS | A cancellation route regression test now asserts exported Ktor OpenTelemetry spans never use `StatusCode.ERROR`; if spans are exported, their status must remain `UNSET`. |
| P1 Coroutine Semantics | PASS | The test uses real `CancellationException` from a Ktor route and preserves the existing Ktor test-host response behavior instead of wrapping cancellation as an application error. |
| P2 Error Boundary | PASS | Existing real 500 request span coverage still asserts `StatusCode.ERROR`, so cancellation and application failure remain distinct. |
| P3 Test Quality | PASS | The regression uses the module's existing `InMemorySpanExporter` helper and bluetape4k assertions. No ad hoc coroutine stress helper is needed because this is single-request instrumentation behavior. |
| P4 Scope Control | PASS | No production code changed. The current OpenTelemetry Ktor behavior already avoids exporting an ERROR span for cancellation; this PR locks that contract with a focused test. |
| P5 Build Hygiene | PASS | Targeted test and full module verification passed. CodeGraph was attempted first but the worktree graph was empty, so review fell back to current source/tests and GNO evidence. |
| P6 Process | PASS | PR metadata must mirror issue #828: milestone `1.11.1`, assignee `debop`, and labels `bug`, `infra/io`, `coroutines`, `codex`, `codex-automation`. |

## Verification

- `./gradlew :bluetape4k-ktor-observability:test --tests "io.bluetape4k.ktor.observability.Bluetape4kKtorObservabilityTest.open telemetry tracing does not record error status for cancellation" --tests "io.bluetape4k.ktor.observability.Bluetape4kKtorObservabilityTest.open telemetry tracing records error request spans" --no-build-cache --no-configuration-cache`
  - PASS: 2 tests.
- `./gradlew :bluetape4k-ktor-observability:compileKotlin :bluetape4k-ktor-observability:compileTestKotlin :bluetape4k-ktor-observability:test --no-build-cache --no-configuration-cache`
  - PASS.
- `./gradlew :bluetape4k-ktor-observability:koverXmlReport --no-configuration-cache`
  - PASS.
- `git diff --check`
  - PASS.

## Residual Risk

- The wrapped OpenTelemetry Ktor instrumentation remains alpha-versioned. If a future version starts exporting a cancellation server span, this test permits it only when the status remains `UNSET`.
