# withKryoAsync Kryo Pool Leak on Cancellation

**Date**: 2026-05-16
**Issue**: #480
**Branch**: `fix/kryo-async-leak`

## Root Cause

`withKryoAsync` obtained the `Kryo` instance **outside** `supplyAsync` and released it via `whenCompleteAsync`:

```kotlin
// BEFORE (leak risk)
val kryo = KryoProvider.obtainKryo()                  // obtained on caller thread
return CompletableFuture.supplyAsync { func(kryo) }
    .whenCompleteAsync { _, _ -> KryoProvider.releaseKryo(kryo) }  // may not run on cancellation
```

Two leak paths:
1. **Cancel before start**: The caller obtains the `Kryo` on its own thread, but if the returned
   future is cancelled before `supplyAsync` runs, `whenCompleteAsync` is never triggered and the
   instance is never released.
2. **`whenCompleteAsync` stage cancelled**: `whenCompleteAsync` returns a new `CompletableFuture`.
   If that stage is cancelled or the callback does not fire (e.g., when the parent stage completes
   exceptionally and the new stage is immediately cancelled), `releaseKryo` is skipped.

## Fix

Move `obtainKryo` and `releaseKryo` **inside** the `supplyAsync` lambda, using `try/finally`:

```kotlin
// AFTER (fix)
return CompletableFuture.supplyAsync {
    val kryo = KryoProvider.obtainKryo()
    try {
        func(kryo)
    } finally {
        KryoProvider.releaseKryo(kryo)
    }
}
```

- If the future is cancelled **before** the supplier runs, `obtainKryo` is never called — no leak.
- If the future is cancelled **during** execution, the supplier continues to completion on the
  worker thread; `finally` runs unconditionally and releases the instance before the worker exits.
- Exception in `func` also triggers `finally`, consistent with the synchronous `withKryo` pattern.

## Test Coverage

New `KryoSupportTest.kt` covers all five helper functions:

- `withKryo` — normal path and exception path (pool returned in both cases)
- `withKryoOutput` — normal path and exception path
- `withKryoInput` — normal path and exception path
- `withKryoAsync` — normal path, null return, exception path (pool not exhausted after 20 failures),
  cancellation path (latch-based synchronization, no `Thread.sleep`)
- `withKryoSuspending` — normal path and exception path

The cancellation test uses a `funcCompleted` `CountDownLatch` signalled from the user-func's
`finally` block. Because the framework's `releaseKryo` executes immediately after the user func
returns (including its `finally`), waiting on `funcCompleted` is sufficient synchronization without
an arbitrary `Thread.sleep`.

## Key Lessons

**Obtain resources inside the task boundary, not outside it.**
When an async task may be cancelled before it starts, any resource obtained before `supplyAsync`
is at risk of leaking. Always scope `obtain`/`release` pairs to the executor lambda, using
`try/finally` to guarantee release on all exit paths (success, exception, interrupt).

**`whenCompleteAsync` does not guarantee callback execution on cancellation.**
It returns a new `CompletableFuture`; if that new stage is cancelled, the callback may not run.
Use `try/finally` inside the supplier instead of a completion callback for resource release.

**`Thread.sleep` in concurrency tests is flaky.**
Use `CountDownLatch` or similar explicit signalling. The `funcCompleted` latch pattern here
(signal from user `finally`, framework release follows immediately) gives deterministic
synchronization without arbitrary delays.

## Verification

```
:bluetape4k-io:test
  KryoSupportTest  12 passing (2s)
  Full module      925 passing (9.7s) — BUILD SUCCESSFUL
```
