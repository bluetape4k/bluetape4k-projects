# Lessons - Issue #856 Workflow ALL Fail-Fast

## Context

Issue #856 fixed `ParallelPolicy.ALL` so blocking and suspend parallel workflows stop remaining siblings when any branch throws or returns a non-success `WorkReport`.

## Lessons

- Fail-fast must cover returned reports as well as thrown exceptions. A child that catches its own exception and returns `WorkReport.Failure` will otherwise look like normal completion to structured concurrency.
- `WorkReport.Aborted` and `WorkReport.Cancelled` are also non-success outcomes for ALL. They should cancel siblings instead of waiting for every branch and applying a priority table afterward.
- Cancellation tests need workflow-owned siblings. Stress helpers are useful for repeated independent execution, but they do not prove that a specific sibling received `InterruptedException` or coroutine cancellation from the workflow scope.
- Suspend implementations must not swallow `CancellationException`. Re-throw it so parent cancellation and child cancellation contracts stay observable.
- Keep README policy text and `ParallelPolicy` KDoc aligned with implementation semantics. ALL means all branches succeed; any non-success branch fails fast.

## Guard

When changing workflow policies, add thrown-exception plus returned `Failure`/`Aborted`/`Cancelled` regression tests for blocking and suspend implementations, then preserve RED and GREEN evidence in `docs/review`.
