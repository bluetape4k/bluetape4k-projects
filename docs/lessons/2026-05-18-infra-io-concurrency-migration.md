# Infra/IO Concurrency Test Migration

**날짜**: 2026-05-18
**이슈**: #532 (#528의 sub-issue)
**브랜치**: test/migrate-infra-io-concurrency

## 범위 확인

`Executors.new*()`와 match된 16개 file을 확인했다:

| 파일 | 패턴 | 결정 |
|------|---------|----------|
| `WorkContextTest.kt` | `Executors.newFixedThreadPool` + `CountDownLatch` | ✅ Migration |
| `RetrofitMetricsSupportTest.kt` | `Executors.newFixedThreadPool` + `CountDownLatch(1)` start latch | ✅ Migration |
| `CompressorEdgeCaseTest.kt` | `Executors.newFixedThreadPool` + `CountDownLatch` | ✅ Migration |
| `SerializerEdgeCaseTest.kt`(test 2개) | `Executors.newFixedThreadPool` + `CountDownLatch` | ✅ Migration |
| `LettuceAtomicLongTest.kt` | `companion executor by lazy` + `CountDownLatch` | ✅ Migration |
| `LettuceLockTest.kt` | `companion executor by lazy` + `CountDownLatch` | ✅ Migration |
| `LettuceSemaphoreTest.kt` | `companion executor by lazy` + `CountDownLatch` | ✅ Migration |
| `VirtualThreads.kt` | Production code | ❌ 유지 |
| `LimitConcurrencyExamples.kt` | Semaphore backpressure demo | ❌ 유지 |
| `FluentAsyncExample.kt` | Async HTTP API에 전달되는 executor | ❌ 유지 |
| `ClientWithRequestFuture.kt` | HTTP client argument로 쓰는 executor | ❌ 유지 |
| `RouteGuideServiceTest.kt` | `asCoroutineDispatcher()` — coroutine dispatcher argument | ❌ 유지 |
| `VertxFutureBulkheadSupportTest.kt` | Bulkhead semantics를 test하는 정확히 2개 task | ❌ 유지 |
| `TimeoutTest.kt` | OkIO library의 `TestingExecutors`(testing timeout behavior) | ❌ 유지 |

## 적용한 migration pattern

### 단순 stress driver(가장 흔함)

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

### workers × rounds pattern(threadCount × iterationsPerThread)

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

### Worker별 identity 보존(`SerializerEdgeCaseTest`)

각 thread에 result tracking용 고유 index가 필요할 때:

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

### `errorCount` pattern 제거

`MultithreadingTester`는 exception을 직접 전파하므로 `AtomicInteger errorCount`가 필요 없다.

```kotlin
// Obsolete
try { work() } catch (e: Throwable) { errorCount.incrementAndGet() } finally { latch.countDown() }
errorCount.get() shouldBeEqualTo 0

// MultithreadingTester: work()의 exception이 test를 직접 실패시킴
```

### Companion `executor by lazy` 제거

Lettuce test는 companion object에 `private val executor by lazy { Executors.newFixedThreadPool(N) }`를
두고 있었다. 모든 test에서 공유되는 형태였지만 실제로는 하나의 stress test에서만 사용했다.
Migration 후 이 field를 완전히 제거했다.

## 유지 신호(non-stress pattern)

| 신호 | 예시 | 결정 |
|--------|---------|----------|
| Async/HTTP API constructor에 전달되는 executor | `Executors.newFixedThreadPool(2)` → `AsyncCloseableHttpClient` | 유지 |
| `.asCoroutineDispatcher()` suffix | gRPC test | 유지 |
| API behavior를 위한 정확히 2개 task(load 아님) | Bulkhead allowed/rejected | 유지 |
| Test framework의 custom executor class | `TestingExecutors.newFixedThreadPool` | 유지 |
| Example 내부 backpressure용 semaphore | Cassandra demo | 유지 |

## 향후 가이드

- `companion object executor by lazy`는 강한 migration 신호다. Test method 간 공유하려고 존재하지만
  실제로 하나의 stress driver test만 사용한다면 그 test를 migrate하고 field를 제거한다.
- `MultithreadingTester` exception은 original을 감싼 `AssertionError`로 전파된다. Error count를 위해
  `add {}` block 내부에 try/catch를 둘 필요가 없다.
- Parameterized test(`@MethodSource`)에서도 `MultithreadingTester`는 동일하게 동작한다.
  Parameter는 lambda closure가 capture한다.
