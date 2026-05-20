# Lesson: Future Wrapper Executor

**Date:** 2026-05-20
**Issue:** #541
**Branch:** `perf/issue-541-future-wrapper`

---

## Context

`FutureToCompletableFutureWrapper` converted plain `Future<T>` instances by creating and
starting a fresh virtual-thread builder in every wrapper instance. This preserved blocking
`Future.get()` semantics but added avoidable allocation and made all watcher threads appear
as the same `future-wrapper` name in diagnostics.

## Decision

Use one shared virtual-thread-per-task executor with a named factory:

- Watcher tasks run through `Executors.newThreadPerTaskExecutor(...)`.
- Thread names use the `future-wrapper-` prefix with per-thread numbering.
- The executor is registered with `ShutdownQueue` to follow the existing core lifecycle pattern.
- `cancel()` still cancels the wrapped `Future` first, then cancels the watcher task and the
  wrapper `CompletableFuture`.

This still uses one virtual thread per active blocking wait. That is intentional for a plain
`Future.get()` adapter: without a callback API, avoiding the wait thread would require a
polling scheduler or a bounded platform-thread pool, both of which are worse tradeoffs here.

## Outcome

The wrapper no longer builds and starts a virtual thread directly for each conversion, while
existing success, exception, and cancellation behavior remains intact. New tests lock the
named virtual watcher and wrapped-future cancellation contracts.

## Verification Evidence

- `./gradlew :bluetape4k-core:compileKotlin :bluetape4k-core:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-core:test --tests 'io.bluetape4k.concurrent.FutureSupportTest' --no-configuration-cache`
- `./gradlew :bluetape4k-core:test --no-configuration-cache`

## Future Guidance

When adapting blocking `Future` APIs into `CompletableFuture`, keep watcher execution behind
a shared lifecycle-managed executor and preserve cancellation propagation to the wrapped
resource. Avoid tests that assert wall-clock performance; prefer deterministic contracts such
as thread naming, virtual-thread usage, completion behavior, and cancellation behavior.
