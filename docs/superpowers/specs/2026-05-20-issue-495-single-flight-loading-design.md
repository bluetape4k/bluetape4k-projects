# Issue 495 Single-flight Loading Design

## Context

Issue #495 asks for reusable single-flight loading across cache and memoizer APIs. The immediate risk is duplicated evaluator execution for the same key and stale write-back after `clear()` while an evaluator is still in flight.

Prior evidence:

- `docs/lessons/2026-05-16-memoizer-clear-race.md` established the generation-token pattern for write-after-clear races.
- `docs/lessons/2026-05-11-cache-lettuce-suspend-close-memoizer.md` established that suspend memoizers must remove failed or cancelled in-flight work and must not swallow `CancellationException`.
- `InMemoryAsyncMemoizer` already has a local in-flight map and generation counter.
- `InMemoryMemoizer` currently uses `ConcurrentHashMap.getOrPut`, which can run the evaluator multiple times under contention.
- `InMemorySuspendMemoizer` shares same-key work through `Deferred`, but `clear()` does not invalidate a running evaluator's later cache write.

## Goal

Introduce one cache-core reusable single-flight primitive and migrate one sync, one `CompletableFuture`, and one suspend implementation to it.

## Scope

In scope:

- Add `SingleFlight` under `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/memoizer`.
- Apply it to:
    - `InMemoryMemoizer`
    - `InMemoryAsyncMemoizer`
    - `InMemorySuspendMemoizer`
- Add focused tests for:
    - same-key single evaluator execution,
    - evaluator failure recovery,
    - Java future null completion,
    - `clear()` during in-flight evaluation,
    - suspend cancellation cleanup.

Out of scope:

- Migrating every cache backend in this issue.
- Adding dependencies.
- Changing public memoizer interfaces.

## Design

`SingleFlight<K, V>` keeps separate in-flight maps for blocking, `CompletableFuture`, and suspend executions. Each new flight receives a `SingleFlightToken` containing the current generation. `clear()` increments the generation first, then clears all in-flight maps.

Callers decide where values are stored. Memoizers use this rule:

1. check result cache before joining or creating a flight,
2. create or join a same-key flight,
3. after evaluator success, write to the result cache only when `singleFlight.isCurrent(token)` is true,
4. always complete the caller with the evaluator result even when the token is stale.

Async Java futures may complete with `null` despite Kotlin non-null generic types. The single-flight path treats null completion as `NullPointerException`, does not cache it, and allows the next call to retry.

Suspend cancellation is not converted into a normal failure. `CancellationException` is completed into the shared in-flight deferred for waiters and then rethrown by the creating coroutine.

## API Decision

Keep the primitive `internal` to `cache-core`. This provides reuse across memoizer implementations without creating a new public API contract or dependency surface.

## Review Notes

Codex review, current session:

- P0: none.
- P1: none after applying the generation-before-clear rule and cancellation rethrow rule.
- P2: The first migration is intentionally limited to in-memory memoizers. Backend memoizers can be migrated in follow-up issues after this primitive is proven.

Claude advisor: not used for this issue per user direction to keep Codex Review in the current session.
