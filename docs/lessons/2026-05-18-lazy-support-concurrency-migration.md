# LazySupportTest Concurrency Migration

**날짜**: 2026-05-18
**이슈**: #529 (#528의 sub-issue)
**브랜치**: test/migrate-lazy-support-concurrency

## 범위 확인

#529의 초기 추정은 약 8개 file이었다. 실제 조사 결과는 다음과 같았다:

| 파일 | 패턴 | 결정 |
|------|---------|----------|
| `LazySupportTest.kt` | `Executors.newFixedThreadPool(8)` + Callable | ✅ Migration |
| `ThreadSupportTest.kt` | `Thread(` | ❌ 유지 — Thread utility test |
| `TimeoutSupportTest.kt` | `Executors.newVirtualThreadPerTaskExecutor()` | ❌ 유지 — test 대상 API의 argument |
| `VirtualThreadSupportTest.kt` | `Thread.ofVirtual(` | ❌ 유지 — virtual thread API test |
| `StructuredConcurrencyTest.kt` | `fork { Thread.currentThread().name }` | ❌ 유지 — API demo |
| `WorkStealingPoolExamples.kt` | `Thread.currentThread().name` | ❌ 유지 — logging reference |
| `StructuredScopesTest.kt` | — | ✅ 이미 `StructuredTaskScopeTester` 사용 |
| `TaskContextTest.kt` | — | ✅ 이미 `StructuredTaskScopeTester` 사용 |

## 결정

`publicLazy` thread-safety test는 `Executors.newFixedThreadPool(8)` + manual `invokeAll` +
`Callable`을 사용했다. 이는 `MultithreadingTester().workers(8).rounds(2)`와 정확히 맞는 raw stress
pattern이다.

Migration 전:

```kotlin
val executor = Executors.newFixedThreadPool(8)
try {
    val tasks = List(16) { Callable { lazyValue } }
    val results = executor.invokeAll(tasks).map { it.get() }.distinct()
    results.size shouldBeEqualTo 1
    results.first() shouldBeEqualTo "value"
} finally {
    executor.shutdownNow()
}
```

Migration 후:

```kotlin
MultithreadingTester()
    .workers(8)
    .rounds(2)
    .add { lazyValue shouldBeEqualTo "value" }
    .run()
```

## 향후 가이드

- Migration candidate를 조사할 때 `Thread(` 단독은 약한 신호다. Test code의 대부분 `Thread(` 사용은
  현재 thread를 확인하거나 Thread API를 test하는 것이며 raw stress driver가 아니다.
- 강한 migration 신호: test method body 내부에서 생성되고 종료되는 `Executors.new*()`와
  `invokeAll` / `submit` 조합.
- Issue scope 추정치는 static grep만 믿지 말고 각 file을 읽어 migration count를 확정한다.
