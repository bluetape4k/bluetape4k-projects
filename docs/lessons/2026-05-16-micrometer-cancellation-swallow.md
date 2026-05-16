# Lesson: Micrometer suspend try* wrappers swallow coroutine cancellation

**Date:** 2026-05-16
**Issue:** #488
**Branch:** `fix/micrometer-cancellation-swallow`
**PR:** (pending)

---

## Root Cause

`tryObserveSuspending` and `tryWithObservationSuspending` wrapped their entire body in
`runCatching {}`. Because `runCatching` is implemented as a blanket `try { } catch (e:
Throwable) { Result.failure(e) }`, it catches `CancellationException` and returns it as
`Result.failure(CancellationException)` instead of rethrowing.

The inner helpers (`withObservationContextSuspending`) already rethrow `CancellationException`
correctly (lines 196-198, 235-237). But the outer `runCatching` intercepted the rethrow and
swallowed it, violating structured concurrency.

---

## Decision

Replace `runCatching` with explicit try/catch that distinguishes cancellation from failure:

```kotlin
return try {
    Result.success(innerCall() ?: throw NoSuchElementException())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
```

The `catch (CancellationException)` block must precede `catch (Throwable)` because
`CancellationException` is a subtype of `IllegalStateException` which is a subtype of
`Throwable`. Ordering matters.

---

## Outcome

- Both `tryObserveSuspending` and `tryWithObservationSuspending` now propagate
  `CancellationException` to the parent job.
- `Result.failure` is returned only for non-cancellation `Throwable`.
- 9 tests pass (7 pre-existing + 2 new cancellation regression tests).

---

## Verification Evidence

```
io.bluetape4k.micrometer.observation.coroutines.ObservationCoroutinesSupportTest ✔ tryObserveSuspending - cancellation propagates to parent job
io.bluetape4k.micrometer.observation.coroutines.ObservationCoroutinesSupportTest ✔ tryWithObservationSuspending - cancellation propagates to parent job
9 passing (2.3s)
```

---

## Future Guidance

- **`runCatching {}` must never wrap `suspend` calls.** It silently converts
  `CancellationException` into `Result.failure`, breaking structured concurrency.
  Use explicit `try/catch` instead.
- **Catch ordering:** `CancellationException` before `Throwable` — always.
- **Test pattern for cancellation propagation:**
  ```kotlin
  var cancelled = false
  var result: Result<T>? = null
  val job = launch {
      try { result = suspendWrapper() }
      catch (e: CancellationException) { cancelled = true; throw e }
  }
  yield(); job.cancel(); job.join()
  cancelled shouldBeEqualTo true
  result shouldBeEqualTo null  // must not have returned Result.failure
  ```
  Asserting both `cancelled == true` AND `result == null` pins the full contract.
