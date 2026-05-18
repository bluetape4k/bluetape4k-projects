# LazySupportTest Concurrency Migration

**Date**: 2026-05-18  
**Issue**: #529 (sub-issue of #528)  
**Branch**: test/migrate-lazy-support-concurrency

## Scope Findings

Initial estimate for #529 was ~8 files. Actual investigation found:

| File | Pattern | Decision |
|------|---------|----------|
| `LazySupportTest.kt` | `Executors.newFixedThreadPool(8)` + Callable | ✅ Migrated |
| `ThreadSupportTest.kt` | `Thread(` | ❌ Keep — tests Thread utilities |
| `TimeoutSupportTest.kt` | `Executors.newVirtualThreadPerTaskExecutor()` | ❌ Keep — argument to API under test |
| `VirtualThreadSupportTest.kt` | `Thread.ofVirtual(` | ❌ Keep — tests virtual thread API |
| `StructuredConcurrencyTest.kt` | `fork { Thread.currentThread().name }` | ❌ Keep — API demo |
| `WorkStealingPoolExamples.kt` | `Thread.currentThread().name` | ❌ Keep — logging reference |
| `StructuredScopesTest.kt` | — | ✅ Already using `StructuredTaskScopeTester` |
| `TaskContextTest.kt` | — | ✅ Already using `StructuredTaskScopeTester` |

## Decision

`publicLazy` thread-safety test used `Executors.newFixedThreadPool(8)` + manual
`invokeAll` + `Callable` — a raw stress pattern that exactly matches
`MultithreadingTester().workers(8).rounds(2)`.

Migration from:
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

Migration to:
```kotlin
MultithreadingTester()
    .workers(8)
    .rounds(2)
    .add { lazyValue shouldBeEqualTo "value" }
    .run()
```

## Future Guidance

- When investigating migration candidates, `Thread(` alone is a weak signal.
  Most `Thread(` usages in test code are inspecting the current thread or testing
  Thread APIs — not raw stress drivers.
- Strong signals for migration: `Executors.new*()` **created and shut down inside
  a test method body** (not in companion `by lazy`) combined with `invokeAll` / `submit`.
- Issue scope estimates from static grep should be validated by reading each file
  before committing to a migration count.
