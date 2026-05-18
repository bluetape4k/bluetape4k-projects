# Cache Memoizer Concurrency Test Migration

**Date**: 2026-05-18
**Issue**: #531 (sub-issue of #528)
**Branch**: test/migrate-cache-memoizer-concurrency

## Scope Findings

| File | Pattern | Decision |
|------|---------|----------|
| `HazelcastMemoizerTest.kt` | `Executors.newFixedThreadPool(16)` + `CountDownLatch` | ✅ Migrated |
| `RedissonMemoizerTest.kt` | `Executors.newFixedThreadPool(16)` + `CountDownLatch` | ✅ Migrated |
| `CaffeineAsyncMemoizerTest.kt` | `CountDownLatch` as async timing barrier | ❌ Keep — not a stress driver |

## Decision

Both `HazelcastMemoizerTest` and `RedissonMemoizerTest` had a test verifying
that a memoizer evaluates the function only once under concurrent load.
The raw pattern used a thread pool + start latch, which is the canonical
stress-driver pattern that maps directly to `MultithreadingTester`.

Migration from:
```kotlin
val pool = Executors.newFixedThreadPool(16)
val startLatch = CountDownLatch(1)
try {
    val tasks = List(16) {
        pool.submit<Int> {
            startLatch.await(1, TimeUnit.SECONDS)
            memoizer(7)
        }
    }
    startLatch.countDown()
    tasks.forEach { it.get(2, TimeUnit.SECONDS) shouldBeEqualTo 49 }
    evaluateCount.get() shouldBeEqualTo 1
} finally {
    pool.shutdownNow()
    map.destroy()
}
```

Migration to:
```kotlin
try {
    MultithreadingTester()
        .workers(16)
        .rounds(1)
        .add { memoizer(7) shouldBeEqualTo 49 }
        .run()
    evaluateCount.get() shouldBeEqualTo 1
} finally {
    map.destroy()
}
```

## CountDownLatch Distinction

`CaffeineAsyncMemoizerTest.kt` uses `CountDownLatch` as an async synchronization
barrier between a background computation and the test assertion — not as a
start-signal for a stress burst. This is a different pattern and should not be
migrated.

Signal patterns for migration candidates:
- `Executors.new*()` **created inside the test body** + `invokeAll` / `submit` + `shutdownNow()` in finally ✅ Migrate
- `CountDownLatch` as start-signal for a pool of concurrent workers ✅ Migrate
- `CountDownLatch` waiting for a single background task to complete ❌ Keep

## Future Guidance

- `evaluateCount` assertions after `MultithreadingTester.run()` remain valid:
  the tester completes all rounds synchronously before returning, so the count
  is stable at assertion time.
- `.rounds(1)` is appropriate when the semantic being tested is "evaluate once
  for a given key", since repeating rounds would reuse the cached value and
  never re-evaluate.
