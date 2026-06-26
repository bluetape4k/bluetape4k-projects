# Issue 797 Review: gRPC client plaintext opt-in

## Scope

- `AbstractGrpcClient`
- gRPC channel security selection helper
- gRPC README locale pair
- gRPC channel security regression tests

## Findings

No P0/P1 findings.

## Checks

- `AbstractGrpcClient(host, port)` now uses transport security by default.
- Plaintext requires explicit `GrpcChannelSecurity.LOCAL_PLAINTEXT`.
- Local plaintext is restricted to loopback hosts.
- Remote plaintext opt-in fails before channel construction.
- README English/Korean examples separate production transport security from local/test plaintext.

## Verification Evidence

- Red test before implementation failed in `:bluetape4k-grpc:compileTestKotlin` because `GrpcChannelSecurity` and `applyGrpcChannelSecurity` did not exist.
- `:bluetape4k-grpc:compileTestKotlin --warning-mode all --rerun-tasks`: passed; remaining warnings are existing Gradle Kotlin DSL deprecations outside the touched gRPC source/test code.
- Targeted gRPC channel security, support validation, and managed channel tests with `--rerun-tasks`: passed.
- Full `:bluetape4k-grpc:test`: 72 tests, 0 failures, 0 errors, 4 skipped.
- `git diff --check`: passed.
- CodeGraph review context: 5 changed files, low risk, 0 impacted nodes reported.

## Residual Risk

Existing local clients that relied on the no-arg or host/port constructor to talk to plaintext local services must pass `GrpcChannelSecurity.LOCAL_PLAINTEXT`. This is the intended security hardening for production-facing defaults.

## Concurrency Helper Gate

No `MultithreadingTester`, `SuspendedJobTester`, or `StructuredTaskScopeTester` coverage was added. The change selects channel transport security during synchronous channel construction and does not add shared mutable state, coroutine lifecycle behavior, or structured task scope behavior.
