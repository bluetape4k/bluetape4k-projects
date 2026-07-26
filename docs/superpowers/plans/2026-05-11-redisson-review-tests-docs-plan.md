# bluetape4k-redisson Review / Tests / Docs Plan

## Steps

1. Record scoped design findings for the redisson-only PR.
2. Add RFuture edge tests for ordering, failure propagation, and coroutine stress via `SuspendedJobTester`.
3. Update public KDoc in `RFutureSupport.kt` with ordering/failure/empty collection contracts.
4. Refresh `README.md` and `README.ko.md`:
    - remove non-existent leader election APIs and diagrams,
    - fix NearCache example to use `defaultLocalCacheOptions`,
    - remove leader from Redis version requirements.
5. Run targeted redisson RFuture tests.
6. Run the redisson module test suite.
7. Apply 6-Tier gate:
    - Tier 1 API/doc contract
    - Tier 2 correctness/edge cases
    - Tier 3 coroutine/concurrency safety
    - Tier 4 integration behavior
    - Tier 5 maintainability/readability
    - Tier 6 release/README accuracy
8. Commit, push, and open a redisson draft PR.

## Stop Condition

The redisson branch has a focused commit, targeted verification evidence, and no known P0/P1 review findings in scope.

## 6-Tier Gate Result

- Tier 1 API/doc contract: PASS. README no longer advertises non-exported leader election APIs; NearCache example uses the actual public API shape.
- Tier 2 correctness/edge cases: PASS. RFuture ordering and failure propagation tests added.
- Tier 3 coroutine/concurrency safety: PASS. RFuture `awaitAll()` has `SuspendedJobTester` stress coverage.
- Tier 4 integration behavior: PASS. Redis-backed redisson test suite passes.
- Tier 5 maintainability/readability: PASS. RFuture KDoc now states ordering, empty input, and failure contracts.
- Tier 6 release/README accuracy: PASS. Redis version table and feature list match the module scope.

Remaining P0/P1 findings: none.
