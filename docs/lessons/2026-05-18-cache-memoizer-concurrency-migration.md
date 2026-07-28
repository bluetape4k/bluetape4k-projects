# Cache Memoizer Concurrency Test Migration

**날짜**: 2026-05-18
**이슈**: #531 (#528의 sub-issue)
**브랜치**: test/migrate-cache-memoizer-concurrency

## 범위 확인

| 파일 | 패턴 | 결정 |
|------|---------|----------|
| `HazelcastMemoizerTest.kt` | `Executors.newFixedThreadPool(16)` + `CountDownLatch` | ✅ Migration |
| `RedissonMemoizerTest.kt` | `Executors.newFixedThreadPool(16)` + `CountDownLatch` | ✅ Migration |
| `CaffeineAsyncMemoizerTest.kt` | Async timing barrier로 쓰는 `CountDownLatch` | ❌ 유지 — stress driver가 아님 |

## 결정

`HazelcastMemoizerTest`와 `RedissonMemoizerTest`에는 concurrent load 아래에서 memoizer가 function을
한 번만 evaluate하는지 검증하는 test가 있었다. Raw pattern은 thread pool + start latch였고,
이는 `MultithreadingTester`로 직접 mapping되는 canonical stress-driver pattern이다.

Migration 전:

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

Migration 후:

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

## CountDownLatch 구분

`CaffeineAsyncMemoizerTest.kt`는 `CountDownLatch`를 background computation과 test assertion 사이의
async synchronization barrier로 사용한다. Stress burst의 start-signal이 아니므로 다른 pattern이며
migration 대상이 아니다.

Migration candidate 신호:

- Test body 내부에서 생성된 `Executors.new*()` + `invokeAll` / `submit` + finally의 `shutdownNow()` ✅ Migration
- Concurrent worker pool의 start-signal로 쓰이는 `CountDownLatch` ✅ Migration
- 단일 background task 완료를 기다리는 `CountDownLatch` ❌ 유지

## 향후 가이드

- `MultithreadingTester.run()` 뒤의 `evaluateCount` assertion은 여전히 유효하다. Tester가 반환되기
  전에 모든 round를 동기적으로 완료하므로 assertion 시점의 count는 안정적이다.
- Test 의미가 "given key에 대해 한 번만 evaluate"라면 `.rounds(1)`이 적절하다. Round를 반복하면
  cached value를 재사용해 다시 evaluate하지 않는다.
