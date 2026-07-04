# Lessons Learned - Issue #821 MongoDB Transaction Abort

## Context

`MongoClient.inTransaction` handled `CancellationException` correctly by
rethrowing it, but called the suspend `ClientSession.abortTransaction()` from
the already-cancelled coroutine context. That could skip the abort cleanup
before MongoDB released the transaction.

## Lesson

Suspend cleanup that must run after coroutine cancellation needs an explicit
`withContext(NonCancellable)` boundary. Keep that boundary as small as possible:
only the cleanup call should run non-cancellable, and the original cancellation
must still be rethrown.

## Outcome

MongoDB transaction abort now runs from `NonCancellable` for both cancellation
and non-cancellation exception paths. Abort failures are preserved as suppressed
exceptions on the owner throwable.

## Future Guard

When a suspend cleanup call runs from a `catch (e: CancellationException)` path,
add a regression test that cancels the current coroutine context before cleanup
and proves a suspension point inside cleanup still completes.

## Verification

- `:bluetape4k-mongodb:compileKotlin` and
  `:bluetape4k-mongodb:compileTestKotlin` passed.
- `:bluetape4k-mongodb:test --tests "io.bluetape4k.mongodb.MongoClientSupportTest"`
  passed with 12 tests.
- `:bluetape4k-mongodb:test` passed with 50 tests.
- `:bluetape4k-mongodb:koverXmlReport` generated the XML coverage report.
- `git diff --check` passed.
