# Issue 543 BehaviorSubject Terminal Cancellation

## Context

`BehaviorSubject.emitError()` rethrew every `CancellationException` raised while
notifying collectors. That made a collector-local cancellation able to interrupt
terminal error notification for the remaining collectors. Claude review also
found the adjacent `complete()` path logged collector-local cancellation as an
error after preserving parent cancellation.

## Decision

Use the same parent-cancellation guard shape for `emitError()` and `complete()`:
catch `CancellationException`, call `currentCoroutineContext().ensureActive()`,
and continue when the emitter coroutine is still active. Avoid broad
`Throwable` catches in newly added coroutine tests when a narrower exception is
expected.

## Outcome

`BehaviorSubject` terminal paths now preserve parent cancellation while allowing
notification to continue after a cancelled collector. Tests cover both
`emitError()` and `complete()` continuation after collector cancellation.

## Verification

`./gradlew :bluetape4k-coroutines:test --tests "io.bluetape4k.coroutines.flow.extensions.subject.SubjectCancellationTest"` passed with 12 tests.

Claude Code Opus rereview reported no remaining P0/P1/P2 findings after
aligning `complete()` and adding the symmetric test. A later
`bluetape4k-patterns` pass narrowed the newly added test catch block from
`Throwable` to `IllegalStateException`.

## Future Guard

Subject terminal paths should distinguish parent coroutine cancellation from
collector-local cancellation before rethrowing `CancellationException`. When
adding coroutine tests, catch only the expected exception unless the test is
explicitly about broad collector failure handling.
