# Flow Operator Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use inline execution in this session. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `bluetape4k-coroutines`에 count-or-time batching/windowing, idle timeout fallback, bounded eager ordered mapping을 추가하고 Rx/Reactor/표준 Flow 대응표와 검증 증거를 제공한다.

**Architecture:** timer 기반 연산자는 `produceIn`/`select`를 이용한 하나의 내부 count-or-time 수집기와 채널 lifecycle로 구현한다. timeout은 upstream producer를 먼저 취소한 뒤 fallback을 한 번만 수집한다. `concatMapEager` bounded overload는 `Semaphore`와 inner별 `Channel`을 사용하며 기존 unbounded overload와 source-order 방출 계약을 유지한다.

**Tech Stack:** Kotlin 2.3, kotlinx.coroutines Flow/Channel/Select, `kotlinx-coroutines-test`, JUnit 5, bluetape assertions, kotlinx-benchmark, Gradle.

---

## 실행 규칙과 순서

- 모든 작업은 `.worktrees/issue-1297-flow-operators`에서 inline으로 수행한다.
- `bluetape-workflow`, `bluetape-kotlin-patterns`, `bluetape-writer`,
  `kotlin-coroutines-skill`, TDD 및 completion verification 지침을 적용한다.
- 테스트는 RED → 최소 구현 → GREEN → refactor 순서로 진행한다.
- Testcontainers/외부 서버는 사용하지 않으며, timer/concurrency 계약은
  `runTest` virtual time와 deterministic markers로 검증한다.
- Operators must not switch dispatchers, call blocking APIs, use `GlobalScope`,
  or detach children; tests collect on the `runTest` dispatcher and cancellation
  markers prove the structured boundary.
- 구현 전 follow-up issue와 inventory를 먼저 고정한다.
- 각 slice는 독립적으로 빌드 가능한 commit을 만들고, stacked PR head는
  앞 slice를 base로 순서대로 올린다.

## Task 1: Follow-up issue와 inventory matrix 고정

**Files:**

- Create: `docs/flow-operator-inventory.md`
- Modify: `bluetape4k/coroutines/README.md`
- Modify: `bluetape4k/coroutines/README.ko.md`
- External: GitHub follow-up issue linked to #1297

- [ ] **Step 1: 중복 issue를 read-only로 확인한다**

Run:

```bash
gh issue list --repo bluetape4k/bluetape4k-projects --state all --limit 100 \
  --search "delay error coroutines"
gh issue list --repo bluetape4k/bluetape4k-projects --state all --limit 100 \
  --search "backpressure overflow coroutines"
```

Expected: #1297과 동일한 delay-error/backpressure 후속 issue가 없다.

- [ ] **Step 2: split follow-up issue를 생성하고 live metadata를 확인한다**

Create one English issue only when Step 1 finds no duplicate:

```bash
gh issue create --repo bluetape4k/bluetape4k-projects \
  --title "enhancement(coroutines): evaluate delay-error and explicit overflow Flow policies" \
  --body $'Parent: #1297\n\n# Scope\nEvaluate delay-error composition and explicit overflow policies after the focused Flow parity slice.\n\n## Deferred families\n- concatDelayError, mergeDelayError, and bounded flatMapDelayError\n- onBackpressureBuffer, onBackpressureLatest, and explicit overflow errors\n- bufferWhen/windowWhen and bufferWhile/windowWhile when caller evidence exists\n\n## Constraints\n- Preserve CancellationException as cancellation.\n- Do not imply Reactive Streams demand semantics for Kotlin Flow.\n- Prefer standard kotlinx.coroutines operators when they already satisfy the contract.\n\n## Acceptance\n- Publish a contract matrix and caller evidence before implementation.\n- Add deterministic failure, cancellation, and bounded-memory tests.\n- Link the final PR back to #1297.'
```

Then verify `state`, `assignees`, `milestone`, and `url` with `gh issue view`.
If the issue is created, run `gh issue edit <number> --assignee debop --milestone
"1.12.0"` and re-read the same fields; do not mutate an existing
issue found by the duplicate search.

- [ ] **Step 3: inventory matrix를 작성한다**

`docs/flow-operator-inventory.md`에는 다음 열과 행을 고정한다.

| Current API | Selected/proposed API | RxJava/Reactor analogue | Standard Flow mapping or non-goal |
|---|---|---|---|
| `chunked`, `windowed` | `bufferTimeout`, `windowTimeout` | `Observable.buffer(timespan,count)`, `Flux.bufferTimeout` | New count-or-time contract |
| none | `timeout`, `timeoutOrFallback` | `Observable.timeout`, `Flux.timeout` | New idle-timeout contract |
| unbounded `concatMapEager` | bounded overload | ordered eager concat family | Custom ordered bounded mapping |
| none | `switchMap` vocabulary decision | `switchMap`, `switchOnNext` | `flatMapLatest`; no wrapper in this issue |
| `merge`, `concat` | delay-error follow-up | `mergeDelayError`, `concatDelayError` | Follow-up issue |
| `onBackpressureDrop` | overflow mapping | Reactor/Rx overflow families | `buffer`/`conflate`; follow-up only |
| `withLatestFrom` | existing API | `withLatestFrom` | Existing single-secondary case |
| none | — | `combine`, `zip`, `retryWhen` | Standard Flow non-goals |

- [ ] **Step 4: 양국어 README에 선택 범위와 표준 Flow 경계를 반영한다**

Add examples that compile against the eventual signatures:

```kotlin
val batches = source.bufferTimeout(maxSize = 100, timeout = 1.seconds)
val windows = source.windowTimeout(maxSize = 100, timeout = 1.seconds)
val recovered = source.timeoutOrFallback(500.milliseconds, fallback = cached)
val ordered = source.concatMapEager(maxConcurrency = 4, bufferCapacity = 8) { load(it) }
```

Document that completion emits a non-empty partial batch/window, upstream
failure drops the in-flight partial value, timeout fallback subscribes after
upstream cancellation, and excluded families map to standard Flow or the
follow-up issue.

- [ ] **Step 5: inventory/document diff를 검증하고 commit한다**

Run:

```bash
git diff --check
rg -n "delayError|backpressure|bufferTimeout|windowTimeout|timeoutOrFallback|concatMapEager" \
  docs/flow-operator-inventory.md bluetape4k/coroutines/README.md bluetape4k/coroutines/README.ko.md
```

Commit:

```bash
git add docs/flow-operator-inventory.md bluetape4k/coroutines/README.md bluetape4k/coroutines/README.ko.md
git commit -m "document selected Flow operator parity contracts" \
  -m "Constraint: Keep standard Flow operators as explicit non-goals.\nRejected: Fold delay-error and overflow policy families into this slice | follow-up contracts are not yet approved.\nConfidence: high\nScope-risk: narrow\nDirective: Keep the inventory synchronized with every public operator change.\nTested: git diff --check and inventory terminology scan.\nNot-tested: New operator tests are added in later slices."
```

## Task 2: Count-or-time buffer/window (RED → GREEN)

**Files:**

- Create: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/bufferTimeout.kt`
- Create: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/BufferTimeoutTest.kt`

- [ ] **Step 1: failing tests for count, timeout, completion, error, cancellation, and virtual time를 작성한다**

The test class uses `runTest`, `assertResult`, and `assertFailure` from the
existing test fixtures. Required cases:

```kotlin
@Test
fun `count boundary closes buffer before timeout`() = runTest {
    flowOf(1, 2, 3, 4, 5)
        .bufferTimeout(maxSize = 2, timeout = 1.hours)
        .assertResult(listOf(1, 2), listOf(3, 4), listOf(5))
}

@Test
fun `virtual time closes a partial buffer`() = runTest {
    val values = flow {
        emit(1)
        delay(100.milliseconds)
        emit(2)
    }.bufferTimeout(maxSize = 10, timeout = 50.milliseconds).toList()

    values shouldBeEqualTo listOf(listOf(1), listOf(2))
}

@Test
fun `completion emits one non empty partial buffer`() = runTest {
    flowOf(1, 2, 3)
        .bufferTimeout(maxSize = 10, timeout = 1.hours)
        .assertResult(listOf(1, 2, 3))
}

@Test
fun `upstream failure drops in flight partial buffer`() = runTest {
    val source = flow<Int> {
        emit(1)
        throw IllegalStateException("boom")
    }
    source.bufferTimeout(10, 1.hours).assertFailure<Int, IllegalStateException>()
}

@Test
fun `window timeout exposes repeatable cold windows`() = runTest {
    val windows = flowOf(1, 2, 3).windowTimeout(2, 1.hours).toList()
    windows.map { it.toList() } shouldBeEqualTo listOf(listOf(1, 2), listOf(3))
    windows.first().toList() shouldBeEqualTo listOf(1, 2)
}

@Test
fun `invalid size and duration fail before collection`() = runTest {
    assertFailsWith<IllegalArgumentException> { flowOf(1).bufferTimeout(0, 1.seconds).toList() }
    assertFailsWith<IllegalArgumentException> { flowOf(1).bufferTimeout(1, Duration.ZERO).toList() }
}

@Test
fun `receive wins a same instant count timeout tie`() = runTest {
    val values = flow {
        emit(1)
        delay(50.milliseconds)
        emit(2)
    }.bufferTimeout(2, 50.milliseconds).toList()

    values shouldBeEqualTo listOf(listOf(1, 2))
}

@Test
fun `take cancellation closes the upstream producer`() = runTest {
    var cancelled = false
    flow {
        try { emit(1); awaitCancellation() }
        finally { cancelled = true }
    }.bufferTimeout(10, 1.hours).take(1).collect()
    cancelled shouldBeEqualTo true
}
```

The receive clause must be registered before `onTimeout` in the implementation
so the same-instant tie test records the documented biased-select rule.

- [ ] **Step 2: targeted RED run을 확인한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.BufferTimeoutTest' \
  --no-configuration-cache --console=plain
```

Expected: compilation or missing-symbol failures for the new APIs.

- [ ] **Step 3: 최소 구현을 작성한다**

Implement `bufferTimeout` and `windowTimeout` using one internal
`countOrTimeout(maxSize, timeout): Flow<List<T>>` collector:

```kotlin
private fun <T> Flow<T>.countOrTimeout(maxSize: Int, timeout: Duration): Flow<List<T>> = flow {
    require(maxSize > 0) { "maxSize must be positive" }
    require(timeout.isPositive()) { "timeout must be positive" }
    coroutineScope {
        val input = this@countOrTimeout.produceIn(this)
        try {
            var current = ArrayList<T>(minOf(maxSize, DEFAULT_BUFFER_CAPACITY))
            whileSelect {
                input.onReceiveCatching { result ->
                    result
                        .onSuccess { value ->
                            current += value
                            if (current.size == maxSize) {
                                emit(current)
                                current = ArrayList(minOf(maxSize, DEFAULT_BUFFER_CAPACITY))
                            }
                        }
                        .onFailure { cause -> cause?.let { throw it } }
                    result.isSuccess
                }
                if (current.isNotEmpty()) {
                    onTimeout(timeout) {
                        emit(current)
                        current = ArrayList(minOf(maxSize, DEFAULT_BUFFER_CAPACITY))
                        true
                    }
                }
            }
            if (current.isNotEmpty()) emit(current)
        } finally {
            input.cancel()
        }
    }
}
```

The implementation must use a fresh list per emission, emit only non-empty
lists, let channel close causes propagate, and convert each list to `asFlow()`
for `windowTimeout`.

- [ ] **Step 4: GREEN targeted run을 확인한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.BufferTimeoutTest' \
  --no-configuration-cache --console=plain
```

Expected: all BufferTimeoutTest cases pass.

- [ ] **Step 5: commit한다**

```bash
git add bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/bufferTimeout.kt \
  bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/BufferTimeoutTest.kt
git commit -m "add count-or-time Flow buffers and windows" \
  -m "Constraint: Timer behavior must remain deterministic under runTest virtual time.\nRejected: Reuse bufferingDebounce | it uses wall-clock System.nanoTime and has different reset semantics.\nConfidence: high\nScope-risk: moderate\nDirective: Preserve non-empty partial-on-completion and drop partial-on-failure semantics.\nTested: BufferTimeoutTest targeted Gradle run.\nNot-tested: Full module check is deferred to the slice gate."
```

## Task 3: Idle timeout and fallback (RED → GREEN)

**Files:**

- Create: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/timeout.kt`
- Create: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/TimeoutTest.kt`

- [ ] **Step 1: failing tests를 작성한다**

```kotlin
@Test
fun `idle timeout starts at collection and resets after each item`() = runTest {
    val error = assertFailsWith<FlowTimeoutException> {
        flow {
            emit(1)
            delay(40.milliseconds)
            emit(2)
            delay(60.milliseconds)
            emit(3)
        }.timeout(50.milliseconds).toList()
    }
    error.timeout shouldBeEqualTo 50.milliseconds
}

@Test
fun `timeout fallback is collected once after upstream cleanup`() = runTest {
    var cleaned = false
    val result = flow {
        try { emit(1); awaitCancellation() } finally { cleaned = true }
    }.timeoutOrFallback(50.milliseconds, flowOf(9, 10)).toList()

    cleaned shouldBeEqualTo true
    result shouldBeEqualTo listOf(1, 9, 10)
}

@Test
fun `normal completion wins over pending timeout`() = runTest {
    flowOf(1, 2).timeout(1.hours).toList() shouldBeEqualTo listOf(1, 2)
}

@Test
fun `caller cancellation is not converted to timeout`() = runTest {
    val job = launch { flow<Int> { awaitCancellation() }.timeout(1.hours).collect() }
    job.cancelAndJoin()
    job.isCancelled shouldBeEqualTo true
}

@Test
fun `invalid timeout is rejected`() = runTest {
    assertFailsWith<IllegalArgumentException> { flowOf(1).timeout(Duration.ZERO).toList() }
}

@Test
fun `fallback failure remains unchanged`() = runTest {
    assertFailsWith<IllegalStateException> {
        flow<Int> { awaitCancellation() }
            .timeoutOrFallback(50.milliseconds, flow { throw IllegalStateException("fallback") })
            .toList()
    }
}
```

- [ ] **Step 2: RED run을 확인한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.TimeoutTest' \
  --no-configuration-cache --console=plain
```

Expected: missing `FlowTimeoutException`/`timeout` symbols.

- [ ] **Step 3: 최소 구현을 작성한다**

Implement a public `FlowTimeoutException : java.util.concurrent.TimeoutException`
(`import java.util.concurrent.TimeoutException`) and two public functions. The
internal loop registers `onTimeout(timeout)` alongside
`onReceiveCatching`; on timeout it marks a flag, cancels the input channel,
then either throws or collects fallback outside the select. Rethrow
`CancellationException` and preserve upstream/fallback causes.

```kotlin
class FlowTimeoutException(val timeout: Duration) : TimeoutException(
    "Flow did not emit within $timeout",
)

fun <T> Flow<T>.timeout(timeout: Duration): Flow<T> =
    timeoutInternal(timeout, fallback = null)

fun <T> Flow<T>.timeoutOrFallback(timeout: Duration, fallback: Flow<T>): Flow<T> =
    timeoutInternal(timeout, fallback)
```

- [ ] **Step 4: GREEN run을 확인하고 commit한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.TimeoutTest' \
  --no-configuration-cache --console=plain
git add bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/timeout.kt \
  bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/TimeoutTest.kt
git commit -m "add idle timeout and fallback Flow operators" \
  -m "Constraint: Upstream cancellation must complete before fallback collection.\nRejected: Expose TimeoutCancellationException as a data-plane error | cancellation would be indistinguishable from caller cancellation.\nConfidence: high\nScope-risk: moderate\nDirective: Always rethrow CancellationException and keep fallback single-subscription.\nTested: TimeoutTest targeted Gradle run.\nNot-tested: Full module check is deferred to the slice gate."
```

## Task 4: Bounded `concatMapEager` (RED → GREEN)

**Files:**

- Modify: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/concatMapEager.kt`
- Create: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/ConcatMapEagerBoundedTest.kt`

- [ ] **Step 1: failing bounded lifecycle tests를 작성한다**

```kotlin
@Test
fun `bounded eager mapping preserves order and limits active inners`() = runTest {
    val active = AtomicInteger(0)
    val peak = AtomicInteger(0)
    val result = flowRangeOf(1, 8)
        .concatMapEager(maxConcurrency = 2, bufferCapacity = 1) { value ->
            flow {
                active.incrementAndGet()
                peak.updateAndGet { maxOf(it, active.get()) }
                emit(value * 10)
                delay(10.milliseconds)
                emit(value * 10 + 1)
                active.decrementAndGet()
            }
        }.toList()

    result shouldBeEqualTo (1..8).flatMap { listOf(it * 10, it * 10 + 1) }
    peak.get() shouldBeLessOrEqualTo 2
}

@Test
fun `bounded eager cancellation stops all inners`() = runTest {
    val cancelled = AtomicInteger(0)
    flowRangeOf(1, 20)
        .concatMapEager(maxConcurrency = 2, bufferCapacity = 1) {
            flow {
                try { emit(it); awaitCancellation() }
                finally { cancelled.incrementAndGet() }
            }
        }.take(1).toList()

    cancelled.get() shouldBeGreaterThan 0
}

@Test
fun `bounded arguments fail before collection`() = runTest {
    assertFailsWith<IllegalArgumentException> {
        flowOf(1).concatMapEager(maxConcurrency = 0) { flowOf(it) }.toList()
    }
    assertFailsWith<IllegalArgumentException> {
        flowOf(1).concatMapEager(maxConcurrency = 1, bufferCapacity = -1) { flowOf(it) }.toList()
    }
}
```

Also assert transform failure and inner failure remain the original exception,
and that every started inner executes its `finally` block after `take(1)`
cancellation.

- [ ] **Step 2: RED run을 확인한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.ConcatMapEagerBoundedTest' \
  --no-configuration-cache --console=plain
```

Expected: no bounded overload exists yet.

- [ ] **Step 3: bounded overload와 channel-backed queue를 구현한다**

Add this overload without changing the existing function:

```kotlin
fun <T : Any, R : Any> Flow<T>.concatMapEager(
    maxConcurrency: Int,
    bufferCapacity: Int = maxConcurrency,
    transform: suspend (T) -> Flow<R>,
): Flow<R> {
    require(maxConcurrency > 0) { "maxConcurrency must be positive" }
    require(bufferCapacity >= 0) { "bufferCapacity must be non-negative" }
    return concatMapEagerInternal(maxConcurrency, bufferCapacity, transform)
}
```

The internal queue uses `Semaphore(maxConcurrency)` and
`Channel<R>(bufferCapacity)`. The old overload delegates to
`Channel.UNLIMITED`/`Int.MAX_VALUE`; inner `finally` closes the channel,
releases the permit, marks completion, and resumes the drain. Do not catch and
swallow child failures.

- [ ] **Step 4: GREEN run과 기존 회귀 테스트를 확인한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.ConcatMapEager*' \
  --no-configuration-cache --console=plain
```

Expected: bounded tests and existing `ConcatMapEagerTest` pass.

- [ ] **Step 5: commit한다**

```bash
git add bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/concatMapEager.kt \
  bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/ConcatMapEagerBoundedTest.kt
git commit -m "bound eager Flow mapping concurrency and queues" \
  -m "Constraint: Preserve the existing source-order overload and structured child lifecycle.\nRejected: Apply a global buffer after concatMapEager | it cannot bound per-inner queues while preserving ordered drain.\nConfidence: medium\nScope-risk: broad\nDirective: Keep semaphore permits and channel cleanup in inner finally blocks.\nTested: bounded and existing ConcatMapEager tests.\nNot-tested: Benchmark evidence is added in the next task."
```

## Task 5: Benchmark and bilingual API documentation

**Files:**

- Modify: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/benchmark/CoroutinesFlowBenchmark.kt`
- Modify: `bluetape4k/coroutines/README.md`
- Modify: `bluetape4k/coroutines/README.ko.md`
- Modify: `docs/flow-operator-inventory.md`

- [ ] **Step 1: benchmark cases를 추가한다**

Add three benchmark methods using existing `runBlocking`/`asFlow` style and
`import kotlin.time.Duration.Companion.days`:

```kotlin
@Benchmark
fun bufferTimeoutThroughput(): Int = runBlocking {
    (1..ITEMS).asFlow()
        .bufferTimeout(100, 1.days)
        .toList()
        .sumOf { it.size }
}

@Benchmark
fun windowTimeoutThroughput(): Int = runBlocking {
    (1..ITEMS).asFlow()
        .windowTimeout(100, 1.days)
        .toList()
        .sumOf { it.toList().size }
}

@Benchmark
fun boundedConcatMapEagerThroughput(): Int = runBlocking {
    (1..TINY_ITEMS).asFlow()
        .concatMapEager(maxConcurrency = 8, bufferCapacity = 8) { n ->
            flowOf(n * 2)
        }
        .toList()
        .size
}
```

The one-day timeout deliberately measures timer registration/list allocation
without
making the benchmark wall-clock dependent; count-boundary and bounded-queue
behavior are the stable evidence for this release.

- [ ] **Step 2: benchmark compile/run을 순차 실행한다**

```bash
./gradlew :bluetape4k-coroutines:testCoroutinesFlowBenchmark \
  --no-configuration-cache --console=plain
```

Expected: benchmark task completes and emits the three new method names.

- [ ] **Step 3: KDoc와 README parity를 마무리한다**

For each public API, include Korean-first KDoc with a runnable example and
explicit completion/error/cancellation/order/buffer/concurrency clauses. Keep
English README and Korean README section order and code signatures identical;
do not introduce diagrams because this slice has no visual contract change.

- [ ] **Step 4: commit한다**

```bash
git add bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/benchmark/CoroutinesFlowBenchmark.kt \
  bluetape4k/coroutines/README.md bluetape4k/coroutines/README.ko.md docs/flow-operator-inventory.md
git commit -m "add Flow operator parity benchmarks and API guidance" \
  -m "Constraint: README locales and KDoc must describe the same public contracts.\nRejected: Add a new diagram for text-only operator contracts | existing diagrams do not encode timer/error details.\nConfidence: high\nScope-risk: narrow\nDirective: Update the inventory whenever a selected operator changes.\nTested: testCoroutinesFlowBenchmark and diff checks.\nNot-tested: External Rx/Reactor runtime integration is outside this module."
```

## Task 6: Verification, review, lesson, and stacked PR delivery

**Files:**

- Create: `docs/reviews/2026-08-03-issue-1297-flow-operator-review.md`
- Create: `docs/lessons/2026-08-03-issue-1297-flow-operator-parity.md`

- [ ] **Step 1: 전체 검증을 순차 실행한다**

```bash
./gradlew :bluetape4k-coroutines:test --no-configuration-cache --console=plain
./gradlew :bluetape4k-coroutines:check --no-configuration-cache --console=plain
./gradlew :bluetape4k-coroutines:testCoroutinesFlowBenchmark \
  --no-configuration-cache --console=plain
git diff --check
```

Expected: all commands succeed; no unrelated module is claimed as tested.

- [ ] **Step 2: final review artifact를 작성한다**

Review the branch diff against the design and record P0/P1/P2/P3 findings,
API compatibility, structured cancellation, virtual-time determinism, queue
bound, README/KDoc parity, benchmark output, and follow-up link. Advancement
requires P0=0 and P1=0.

- [ ] **Step 3: lesson을 작성하고 commit한다**

Record the timer race decision, why `bufferingDebounce` was not reused, the
fallback cleanup proof, and the bounded queue trade-off with exact commands.

- [ ] **Step 4: PR train을 생성한다**

Push the feature branch and create PRs in dependency order. Each PR must:

- target `develop` or the immediately previous PR head as appropriate;
- be assigned to `debop`, mirror issue milestone/labels, and link `#1297`;
- use an English title/body ending with `## DoD Status`;
- include exact test commands and the current commit SHA.

Before creation, verify `git status`, `git log`, branch head, and the issue
metadata. Do not merge any PR in this task without a fresh exact-head approval.

- [ ] **Step 5: merge-ready DoD를 보고하고 중단한다**

Run `gh pr view`, `gh pr checks`, review/thread queries, and mergeability checks
for the current top head. Report plan-item status, changed files, required
checks `X/Y`, N/A and blocked counts, known risks, exact PR/head, and unchecked
merge gates. Stop at `PENDING` awaiting explicit merge approval.

## Plan self-review

- Spec coverage: Tasks 1–5 cover inventory, all three selected API families,
  docs, tests, benchmark evidence, cancellation, and follow-up split.
- Placeholder scan: no unresolved placeholders or unspecified validation commands.
- Boundary scan: timer start, same-instant select precedence, cold-window
  repeatability, invalid arguments, and cancellation markers have exact tests.
- Type consistency: signatures use `Duration`, `FlowTimeoutException`,
  `timeoutOrFallback`, and bounded `concatMapEager` consistently across tasks.
- Scope: no new module/dependency; all writes are within the coroutines module,
  docs, tests, and review/lesson artifacts.
