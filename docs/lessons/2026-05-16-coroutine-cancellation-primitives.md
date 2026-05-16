# Lesson: Coroutine Cancellation Primitives — Stale Waiters and Uncancelled Futures

**Date:** 2026-05-16
**Issue:** #483
**Branch:** `fix/coroutine-cancellation-primitives`
**PR:** (pending)

---

## Root Cause

### Bug 1 — Resumable stale waiter

`Resumable.await()` installs a `Continuation` into `continuationRef` via CAS inside
`suspendCancellableCoroutine`, but previously registered no `invokeOnCancellation` handler.

When the awaiting coroutine was cancelled before `resume()` was called, the continuation
remained in the slot. A subsequent `await()` call found the slot occupied and threw
`IllegalStateException("Only one thread can await a Resumable")`, even though the original
awaiter was long gone.

### Bug 2 — FutureToCompletableFutureWrapper uncancelled underlying Future

`FutureToCompletableFutureWrapper` (in `bluetape4k-core`) wraps a plain `Future<T>` using a
virtual thread that blocks on `future.get()`. The class did not override `cancel()`, so
coroutine cancellation (which calls `CompletableFuture.cancel()`) only marked the wrapper as
cancelled — the virtual thread continued blocking on `future.get()` indefinitely, leaking the
underlying work.

---

## Decision

**Bug 1 fix** — register `invokeOnCancellation` immediately after the CAS succeeds:

```kotlin
if (continuationRef.compareAndSet(current, cont)) {
    // CAS-clear only this cont; leave READY intact if resume() already replaced it.
    cont.invokeOnCancellation {
        continuationRef.compareAndSet(cont, null)
    }
    break
}
```

Key invariant: `compareAndSet(cont, null)` (not `set(null)`) preserves a READY sentinel
placed by a concurrent `resume()`. If `resume()` wins first, the CAS comparand is READY ≠
cont, so the cancellation handler is a no-op and READY is unaffected.

**Bug 2 fix** — override `cancel()` in `FutureToCompletableFutureWrapper`:

```kotlin
override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
    wrapped.cancel(mayInterruptIfRunning)
    return super.cancel(mayInterruptIfRunning)
}
```

Order matters: cancel `wrapped` first so the virtual thread's `future.get()` unblocks,
then call `super.cancel()`. The virtual thread may re-enter `cancel(true)` from its
`CancellationException` catch block — this is safe because `CompletableFuture.cancel()` is
idempotent on already-cancelled state.

---

## Outcome

- `Resumable`: cancelled `await()` now clears the slot; next `await()` + `resume()` pair
  succeeds without `IllegalStateException`.
- `FutureToCompletableFutureWrapper`: coroutine cancellation propagates to the underlying
  `Future`, stopping the virtual thread's blocking work.
- 7 tests pass (5 pre-existing + 2 new for `Resumable`, 4 pre-existing + 1 new for
  `FutureSupport`).

---

## Verification Evidence

```
io.bluetape4k.coroutines.flow.extensions.ResumableTest ✔ correct state
io.bluetape4k.coroutines.flow.extensions.ResumableTest ✔ cancelled await clears slot so subsequent await succeeds
io.bluetape4k.coroutines.flow.extensions.ResumableTest ✔ READY fast path still works after invokeOnCancellation change
io.bluetape4k.coroutines.support.FutureSupportTest     ✔ Massive future as CompletableFuture in multi-threads
io.bluetape4k.coroutines.support.FutureSupportTest     ✔ Massive Future as CompletableFuture in Coroutines
io.bluetape4k.coroutines.support.FutureSupportTest     ✔ 취소된 Future는 await 시 CancellationException을 던진다
io.bluetape4k.coroutines.support.FutureSupportTest     ✔ awaitSuspending 취소 시 하위 Future도 취소된다
7 passing (2.1s)
```

---

## Review Findings Resolved

| Severity | Finding | Resolution |
|----------|---------|------------|
| LOW | KDoc language mix in `Resumable.await()` | Converted full block to English |
| LOW | KDoc language mix in `awaitSuspending()` | Converted full block to English |
| LOW | Test `catch (CancellationException)` without rethrow | Removed try/catch entirely |
| LOW | Magic `50.milliseconds` literal in test | Extracted to `CANCEL_PROPAGATION_DELAY_MS` const |

---

## Future Guidance

- **`suspendCancellableCoroutine` + CAS install pattern**: always register
  `invokeOnCancellation` using `compareAndSet(cont, null)` — not `set(null)` — so that a
  concurrent sentinel set by a producer is not clobbered.
- **`invokeOnCancellation` fires synchronously** when the coroutine is already cancelled at
  registration time, so there is no "registered too late" window.
- **READY fast path does not suspend**: when `cont.resumeWith()` fires inside the CancellableCoroutine
  lambda, no cancellation window exists and `invokeOnCancellation` need not be registered.
- **CompletableFuture wrappers must override `cancel()`** if they launch background threads or
  virtual threads that block on the wrapped resource. Omitting the override leaks the thread.
- **cancel order in wrappers**: cancel the wrapped resource first, then call
  `super.cancel()`. The wrapper's virtual thread can re-enter `cancel()` from its catch block;
  this is safe because `CompletableFuture.cancel()` is idempotent.
