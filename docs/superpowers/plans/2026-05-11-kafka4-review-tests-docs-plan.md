# infra/kafka4 Review, Tests, and Docs Plan

## Plan

1. Add a public codec matrix to README and README.ko.
2. Update public KDoc examples and `suspendSend` failure/cancellation contract.
3. Add unit-level edge tests for `suspendSend` callback failure, cancellation, and coroutine stress behavior.
4. Run local CI-light verification.
   - `git diff --check`
   - `./gradlew :bluetape4k-kafka4:compileTestKotlin --no-build-cache`
5. Commit, push, and open a draft PR so GitHub CI validates the module.

## 6-Tier Gate

### Tier 1 - Public API contracts

No public signatures are changed. KDoc is updated to document existing behavior.

### Tier 2 - Correctness and edge cases

Callback failure and cancellation paths for `suspendSend` are directly covered.

### Tier 3 - Concurrency and coroutine safety

`SuspendedJobTester` is used for repeated concurrent suspend sends, following the bluetape4k test guidance.

### Tier 4 - Documentation and examples

README and README.ko now expose the actual codec constants and Fory-oriented examples.

### Tier 5 - Build and verification

Local verification is limited to test compilation and patch checks; GitHub Actions remains the authoritative CI gate.

### Tier 6 - Maintainability

Edge tests use mock Kafka producers so they do not add broker startup cost or timing sensitivity.

## Gate Status

- P0: none
- P1: none after planned README codec matrix update
- P2: coroutine edge tests and Fory-oriented KDoc examples addressed

