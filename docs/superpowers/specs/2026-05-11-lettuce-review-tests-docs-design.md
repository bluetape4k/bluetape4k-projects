# bluetape4k-lettuce Review / Tests / Docs Design

## Scope

- Module: `infra/lettuce`
- Goal: close review findings, add missing edge tests, strengthen public API KDoc/examples, and refresh README files so a standalone lettuce PR can be opened.
- Skills: `bluetape4k-design` 6-Tier gate and `bluetape4k-patterns`.

## Findings

### P1 - README advertises non-existent public APIs

`README.md` and `README.ko.md` list `LettuceLeaderElection`, `LettuceSuspendLeaderElection`, `LettuceLeaderGroupElection`, and `LettuceSuspendLeaderGroupElection`, but `infra/lettuce/src/main/kotlin` has no `leader` package. The feature table, examples, and Mermaid diagram must be limited to APIs exported by this module.

### P1 - README links to moved cache-lettuce documentation incorrectly

The Memoizer note links to `../cache-lettuce/README*.md` from `infra/lettuce`, but the target module lives at `../../cache/cache-lettuce`.

### P2 - KDoc pipeline example contradicts module guidance

`withPipeline` KDoc still demonstrates per-future `async { await() }`, while the README now recommends `Collection<RedisFuture>.awaitAll()` to avoid spawning one coroutine per future.

### P2 - RedisFuture adapters lack direct edge coverage

Existing Redis-backed tests cover success paths and empty collections. Missing edge coverage:

- `sequence()` preserves input ordering even when futures complete out of order.
- `awaitAll()` propagates failed futures.
- coroutine stress around `awaitAll()` uses `SuspendedJobTester`, as required for coroutine-safety test additions.

## Acceptance Criteria

- Public docs mention only APIs exported from `infra/lettuce`, or explicitly state cross-module ownership.
- KDoc examples use `awaitAll()` outside `withPipeline`.
- RedisFuture edge tests compile and pass.
- `./gradlew :bluetape4k-lettuce:test` passes, or any infrastructure blocker is recorded.
- 6-Tier review has no remaining P0/P1 findings for the lettuce PR scope.
