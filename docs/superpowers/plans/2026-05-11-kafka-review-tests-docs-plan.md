# infra/kafka Review, Tests, and Docs Plan

## Plan

1. Align public documentation with the actual codec API.
   - Replace stale FST references with Fory.
   - Add the missing compressed codec constants to English/Korean README tables and diagrams.
2. Harden public KDoc.
   - Update `KafkaCodecs` binary codec description and example.
   - Document `suspendSend` failure and cancellation contract.
   - Fix `topicPartitionOf` validation helper naming.
3. Add edge tests.
   - `suspendSend` propagates callback failures.
   - cancelling the coroutine cancels the returned Kafka future.
   - `SuspendedJobTester` exercises repeated concurrent suspend sends.
4. Run the local CI-light gate.
   - `git diff --check`
   - inspect `git diff --stat`
5. Commit, push, and open a draft PR so GitHub CI validates the module.

## 6-Tier Gate

### Tier 1 - Public API contracts

No public signatures are changed. KDoc is updated to match existing behavior.

### Tier 2 - Correctness and edge cases

`suspendSend` callback failure and cancellation paths are now covered directly.

### Tier 3 - Concurrency and coroutine safety

`SuspendedJobTester` is used for repeated concurrent suspend sends, following the bluetape4k test guidance.

### Tier 4 - Documentation and examples

README and README.ko are synchronized with `KafkaCodecs` public constants. FST references are removed from this module's public docs.

### Tier 5 - Build and verification

Local verification stays lightweight because the user requested CI-only validation. GitHub Actions is the authoritative test gate for this PR.

### Tier 6 - Maintainability

Tests use mock Kafka producers for edge behavior so they do not add broker startup cost or timing sensitivity.

## Gate Status

- P0: none
- P1: none after planned documentation updates
- P2: coroutine edge tests and stale helper KDoc addressed

