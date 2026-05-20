# Issue 495 Single-flight Loading Plan

## Work Type

Type A - Full Design, narrowed to one cache-core migration slice.

## Tasks

- [x] Confirm issue requirements and prior lessons.
- [x] Inspect current in-memory sync, async, and suspend memoizers.
- [x] Inspect references with IDE tooling where possible; fallback to `rg` because IDE reference lookup was indexing.
- [x] Add `SingleFlight` with sync, async, and suspend execution paths.
- [x] Migrate `InMemoryMemoizer`, `InMemoryAsyncMemoizer`, and `InMemorySuspendMemoizer`.
- [x] Add focused unit tests for same-key coalescing, clear races, null future completion, and cancellation/failure recovery.
- [x] Run IDE diagnostics/import optimization for touched Kotlin files.
- [x] Run targeted cache-core tests and `git diff --check`.
- [x] Run current-session Codex Review on the final diff.
- [ ] Add lesson, commit, push, create PR, and check CI.

## Verification Plan

Run:

```bash
./gradlew :bluetape4k-cache-core:compileKotlin :bluetape4k-cache-core:compileTestKotlin --no-configuration-cache
./gradlew :bluetape4k-cache-core:test --tests '*SingleFlightTest' --tests '*InMemory*MemoizerTest' --no-configuration-cache
git diff --check
```

If the targeted test command misses inherited fixture behavior, run the full cache-core test task.

## Review Gates

Spec review, current-session Codex:

- P0: none.
- P1: none.
- P2: backend-wide migration is deferred; this plan satisfies issue acceptance with one sync, one async, and one suspend implementation.

Plan review, current-session Codex:

- P0: none.
- P1: none.
- P2: concurrent `clear()` is generation-based, not a full linearizable cancellation barrier. This matches existing cache memoizer behavior and keeps the contract small.

Code review, current-session Codex:

- P0: none.
- P1: none.
- P2: blocking single-flight concurrency tests originally used short latch timeouts and failed under full-suite load. The tests were revised to keep the first evaluator blocked until the second call has time to join the active flight.

Claude advisor: skipped per user instruction to handle Codex Review in this session.

## Validation Evidence

Passed:

```bash
./gradlew :bluetape4k-cache-core:compileKotlin :bluetape4k-cache-core:compileTestKotlin --no-configuration-cache
./gradlew :bluetape4k-cache-core:test --tests '*SingleFlightTest' --tests '*InMemory*MemoizerTest' --no-configuration-cache
./gradlew :bluetape4k-cache-core:test --no-configuration-cache
git diff --check
```

Notes:

- Full cache-core test result: 464 passing.
- Compile emitted two pre-existing warnings in `BackJCacheCommandTest`; no touched-file diagnostics errors were reported.
