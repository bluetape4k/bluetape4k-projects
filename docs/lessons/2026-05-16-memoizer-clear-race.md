# Memoizer Write-after-clear Race Condition Fix

**Date**: 2026-05-16
**Issue**: #487
**Branch**: `fix/memoizer-clear-race`

## Root Cause

Async and suspend memoizers shared a common bug: an in-flight evaluator that
completed *after* `clear()` was called would write its result back into the
cache, repopulating it with stale data. Callers who triggered `clear()` to
invalidate the cache got a fresh result on the very next call, but other callers
who had started evaluation before `clear()` would silently refill the cache.

A secondary bug existed in `CaffeineMemoizer.clear()`: it called
`cache.cleanUp()` (maintenance pass) instead of `cache.invalidateAll()`
(actual eviction), making `clear()` effectively a no-op.

## Fix

### Generation counter pattern

All five affected memoizers received an `AtomicLong generation` counter:

1. `generation.get()` is captured into `capturedGen` **before** the evaluator
   starts (or the in-flight future is shared).
2. `clear()` calls `generation.incrementAndGet()` **first**, then clears the
   cache and in-flight maps.
3. Before writing to the cache after evaluation, the code checks
   `generation.get() == capturedGen`. If they differ, the result was computed
   before a `clear()` — the write is skipped, but the caller's future/deferred
   is still completed with the computed value.

### Value-aware ConcurrentHashMap.remove

In-flight map cleanup uses the 2-argument `remove(key, value)` form to avoid
evicting a freshly-installed promise that races with completion from a previous
generation.

### Null future completion

`InMemoryAsyncMemoizer` and the Caffeine/EhCache/JCache variants now call
`completeExceptionally(NullPointerException(...))` when the evaluator's
`CompletableFuture` completes with a `null` value, preventing an indefinitely
uncompleted promise.

### CaffeineMemoizer.clear() bug

`cache.cleanUp()` → `cache.invalidateAll()`. Also removed the unnecessary
`ReentrantLock` that guarded only a maintenance call.

### Affected files

| File | Change |
|------|--------|
| `CaffeineAsyncMemoizer.kt` | generation counter, value-aware remove, null check, `invalidateAll()` |
| `EhCacheAsyncMemoizer.kt` | generation counter, value-aware remove, null check |
| `JCacheAsyncMemoizer.kt` | generation counter, value-aware remove, null check |
| `InMemoryAsyncMemoizer.kt` | generation counter, value-aware remove, null check |
| `Cache2kSuspendMemoizer.kt` | generation counter, value-aware remove |
| `CaffeineMemoizer.kt` | `clear()` uses `invalidateAll()`; lock removed |

## Test Coverage

New tests added to `CaffeineAsyncMemoizerTest` and `Cache2KSuspendMemoizerTest`:

- `clear 후 캐시가 무효화된다` — basic clear invalidates cache, triggers fresh evaluation
- `clear 중 진행 중인 비동기 결과는 캐시에 저장되지 않는다` — in-flight result not
  written back after `clear()` (verified via `getIfPresent` returning null)
- `clear 중 진행 중인 suspend 결과는 캐시에 저장되지 않는다` — same guarantee for
  suspend memoizer (verified via second evaluation being triggered)

## Verification

```
447 passing (40.8s) — BUILD SUCCESSFUL
```

## Key Lesson

**Increment generation before clearing**, not after. If `clear()` clears the
cache and then increments generation, a concurrent evaluator can check
generation (still old), see equality, and write after the increment completes.
The correct order is: increment → clear.

**`cache.cleanUp()` is not `cache.invalidateAll()`**. Caffeine's `cleanUp()`
triggers pending maintenance tasks (expiry sweeps, etc.); it does not evict all
entries. Always use `invalidateAll()` when the intent is to empty the cache.
