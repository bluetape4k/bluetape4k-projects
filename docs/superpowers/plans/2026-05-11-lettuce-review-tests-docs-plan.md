# bluetape4k-lettuce Review / Tests / Docs Plan

## Steps

1. Record scoped design findings for the lettuce-only PR.
2. Add RedisFuture edge tests for ordering, failure propagation, and coroutine stress via `SuspendedJobTester`.
3. Update public KDoc in `RedisFutureSupport.kt` and `withPipeline` with Korean contracts and current examples.
4. Refresh `README.md` and `README.ko.md`:
    - remove non-existent leader election APIs and diagrams,
    - fix cache-lettuce links,
    - clarify Protobuf codec ownership,
    - align Korean codec diagram with current FastFory factories.
5. Run targeted lettuce tests.
6. Apply 6-Tier gate:
    - Tier 1 API/doc contract
    - Tier 2 correctness/edge cases
    - Tier 3 coroutine/concurrency safety
    - Tier 4 integration behavior
    - Tier 5 maintainability/readability
    - Tier 6 release/README accuracy
7. Commit, push, and open a lettuce draft PR.

## Stop Condition

The lettuce branch has a focused commit, targeted verification evidence, and no known P0/P1 review findings in scope.

## 6-Tier Gate Result

- Tier 1 API/doc contract: PASS. README no longer advertises non-exported leader election APIs; Protobuf codec ownership is cross-module and explicit.
- Tier 2 correctness/edge cases: PASS. RedisFuture ordering and failure propagation tests added.
- Tier 3 coroutine/concurrency safety: PASS. RedisFuture `awaitAll()` has `SuspendedJobTester` stress coverage.
- Tier 4 integration behavior: PASS. Redis-backed lettuce test suite passes.
- Tier 5 maintainability/readability: PASS. KDoc examples now use the module-level `awaitAll()` pattern.
- Tier 6 release/README accuracy: PASS. cache-lettuce links point to the actual module path.

Remaining P0/P1 findings: none.
