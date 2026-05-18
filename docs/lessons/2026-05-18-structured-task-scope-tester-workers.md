# StructuredTaskScopeTester workers() Concurrency Control

**Date**: 2026-05-18  
**Issue**: #522  
**Branch**: fix/structured-task-scope-workers

## Root Cause

`StructuredTaskScopeTester` implemented `StressTester` (rounds only) and had no `workers()` method.
All forked virtual threads ran without any concurrency cap, making `workers(n)` silently ineffective.

## Decision

- Upgrade to `WorkerStressTester<StructuredTaskScopeTester>` to gain the `workers()` contract.
- Add an internal `Semaphore(workerSize)` acquired **inside** each forked task (not before fork).
  - Acquiring inside the fork lets all tasks be submitted immediately; the semaphore gates actual execution.
  - `finally { semaphore.release() }` guarantees release even on exception.
  - On scope cancellation, threads blocked in `semaphore.acquire()` receive `InterruptedException` before
    a permit is taken, so no permit leaks.
- Default: `Runtime.getRuntime().availableProcessors() * 2` (mirrors issue acceptance criteria).
  - `Systemx.availableProcessors` was the first choice but `bluetape4k-core` is not a dependency
    of `bluetape4k-junit5`; use `Runtime` directly instead of adding the dependency.

## Verification

- 10 tests in `StructuredTaskScopeTesterTest` passing (0 failures).
- 3 tests in `StressTesterContractTest` passing (updated to use `configureWorkerTester`).

## Review Findings Resolved

| Finding | Fix |
|---------|-----|
| Non-atomic peak-concurrency measurement (`incrementAndGet` + `getAndUpdate`) | Replaced with `Semaphore.tryAcquire()` inside the block — fails atomically if over limit |
| `StressTesterContractTest` still used `configureRoundsTester` after interface upgrade | Updated to `configureWorkerTester(workers=2, rounds=4)` |

## Future Guidance

- When a tester gains `workers()`, always upgrade the contract test to `configureWorkerTester`.
- For concurrency-limit tests, prefer `Semaphore.tryAcquire()` over an atomic counter + peak-update
  pair — the two-step pattern has a TOCTOU gap that can hide semaphore bugs.
- Do not depend on `bluetape4k-core` from `bluetape4k-junit5`; use JDK stdlib equivalents.
