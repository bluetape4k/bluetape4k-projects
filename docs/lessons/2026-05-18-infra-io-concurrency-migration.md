# Infra/IO Concurrency Test Migration

**Date**: 2026-05-18
**Issue**: #532 (sub-issue of #528)
**Branch**: test/migrate-infra-io-concurrency

## Scope Findings

From 16 files matching `Executors.new*()`:

| File | Pattern | Decision |
|------|---------|----------|
| `WorkContextTest.kt` | `Executors.newFixedThreadPool` + `CountDownLatch` | ✅ Migrated |
| `RetrofitMetricsSupportTest.kt` | `Executors.newFixedThreadPool` + `CountDownLatch(1)` start latch | ✅ Migrated |
| `CompressorEdgeCaseTest.kt` | `Executors.newFixedThreadPool` + `CountDownLatch` | ✅ Migrated |
| `SerializerEdgeCaseTest.kt` (×2 tests) | `Executors.newFixedThreadPool` + `CountDownLatch` | ✅ Migrated |
| `LettuceAtomicLongTest.kt` | `companion executor by lazy` + `CountDownLatch` | ✅ Migrated |
| `LettuceLockTest.kt` | `companion executor by lazy` + `CountDownLatch` | ✅ Migrated |
| `LettuceSemaphoreTest.kt` | `companion executor by lazy` + `CountDownLatch` | ✅ Migrated |
| `VirtualThreads.kt` | Production code | ❌ Keep |
| `LimitConcurrencyExamples.kt` | Semaphore backpressure demo | ❌ Keep |
| `FluentAsyncExample.kt` | Executor passed to async HTTP API | ❌ Keep |
| `ClientWithRequestFuture.kt` | Executor as HTTP client argument | ❌ Keep |
| `RouteGuideServiceTest.kt` | `asCoroutineDispatcher()` — coroutine dispatcher arg | ❌ Keep |
| `VertxFutureBulkheadSupportTest.kt` | Exactly 2 tasks testing bulkhead semantics | ❌ Keep |
| `TimeoutTest.kt` | `TestingExecutors` from OkIO library (testing timeout behavior) | ❌ Keep |

## Migration Patterns Applied

### Simple stress driver (most common)
```kotlin
// Before
val executor = Executors.newFixedThreadPool(N)
val latch = CountDownLatch(N)
repeat(N) {
    executor.submit { work(); latch.countDown() }
}
latch.await(30, TimeUnit.SECONDS)
executor.shutdown()

// After
MultithreadingTester()
    .workers(N)
    .rounds(1)
    .add { work() }
    .run()
```

### workers × rounds pattern (threadCount × iterationsPerThread)
```kotlin
// Before
repeat(threadCount) {
    executor.submit {
        repeat(iterationsPerThread) { ctx.compute(...) }
        latch.countDown()
    }
}

// After
MultithreadingTester()
    .workers(threadCount)
    .rounds(iterationsPerThread)
    .add { ctx.compute(...) }
    .run()
```

### Per-worker identity preservation (SerializerEdgeCaseTest)
When each thread needs a unique index for result tracking:
```kotlin
val counter = AtomicInteger(0)
val results = ConcurrentHashMap<Int, Item>()
MultithreadingTester()
    .workers(THREAD_COUNT)
    .rounds(1)
    .add {
        val idx = counter.getAndIncrement()
        val item = Item(idx, "thread-$idx")
        val bytes = serializer.serialize(item)
        results[idx] = serializer.deserialize(bytes)
    }
    .run()
results.size shouldBeEqualTo THREAD_COUNT
```

### Removed errorCount pattern
`MultithreadingTester` propagates exceptions directly — no need for `AtomicInteger errorCount`. The pattern:
```kotlin
// Obsolete
try { work() } catch (e: Throwable) { errorCount.incrementAndGet() } finally { latch.countDown() }
errorCount.get() shouldBeEqualTo 0

// MultithreadingTester: exception from work() fails the test directly
```

### companion `executor by lazy` removal
Lettuce tests had `private val executor by lazy { Executors.newFixedThreadPool(N) }` in companion objects — shared across all tests but only used in one stress test. After migration, the field was removed entirely.

## Keep Signals (non-stress patterns)

| Signal | Example | Decision |
|--------|---------|----------|
| Executor passed to async/HTTP API constructor | `Executors.newFixedThreadPool(2)` → `AsyncCloseableHttpClient` | Keep |
| `.asCoroutineDispatcher()` suffix | gRPC test | Keep |
| Exactly 2 tasks for API behavior (not load) | Bulkhead allowed/rejected | Keep |
| Custom executor class from test framework | `TestingExecutors.newFixedThreadPool` | Keep |
| Semaphore for backpressure inside example | Cassandra demo | Keep |

## Future Guidance

- `companion object executor by lazy` is a strong migration signal — it exists only to be shared across test methods, but if only one test actually uses it as a stress driver, migrate that test and remove the field.
- `MultithreadingTester` exceptions propagate as `AssertionError` wrapping the original — no need for try/catch inside `add {}` blocks for error counting.
- For parameterized tests (`@MethodSource`), `MultithreadingTester` works identically — the parameter is captured by the lambda closure.
