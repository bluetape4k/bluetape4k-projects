# Flow 연산자 동등성 구현 계획

> **에이전트 작업 지침:** 이 세션에서 inline으로 실행한다. 진행 상태는 checkbox(`- [ ]`) 형식으로 추적한다.

**목표:** `bluetape4k-coroutines`에 count-or-time batching/windowing, idle timeout fallback, bounded eager ordered mapping을 추가하고 Rx/Reactor/표준 Flow 대응표와 검증 근거를 제공한다.

**아키텍처:** Count-or-time 연산자는 `produceIn`/`select`를 이용한 내부
collector 하나와 channel 수명주기로 구현한다. 유휴 timeout은 명시적 upstream
`Job`과 `Channel`을 `select`로 감시하고, timeout 시 `cancelAndJoin`을 완료한 뒤
fallback을 한 번만 수집한다. `concatMapEager`의 제한된 overload는 `Semaphore`와
내부 항목별 `Channel`을 사용하며 기존 무제한 overload와 원본 순서 방출 계약을
유지한다.

**기술 스택:** Kotlin 2.3, kotlinx.coroutines Flow/Channel/Select, `kotlinx-coroutines-test`, JUnit 5, bluetape assertion, kotlinx-benchmark, Gradle

---

## 실행 규칙과 순서

- 모든 작업은 `.worktrees/issue-1297-flow-operators`에서 inline으로 수행한다.
- `bluetape-workflow`, `bluetape-kotlin-patterns`, `bluetape-writer`,
  `kotlin-coroutines-skill`, TDD 및 completion verification 지침을 적용한다.
- 테스트는 RED → 최소 구현 → GREEN → 리팩터링 순서로 진행한다.
- Testcontainers/외부 서버는 사용하지 않으며, timer/concurrency 계약은
  `runTest` virtual time와 deterministic markers로 검증한다.
- 연산자는 dispatcher를 전환하거나 blocking API와 `GlobalScope`를 사용하거나
  child를 분리해서는 안 된다. 테스트는 `runTest` dispatcher에서 수집하고
  cancellation marker로 구조화된 경계를 증명한다.
- 구현 전 follow-up issue와 inventory를 먼저 고정한다.
- 각 slice는 독립적으로 빌드 가능한 commit을 만들고, stacked PR head는
  앞 slice를 base로 순서대로 올린다.

## 작업 1: 후속 issue와 inventory matrix 고정

**대상 파일:**

- 생성: `docs/flow-operator-inventory.md`
- 수정: `bluetape4k/coroutines/README.md`
- 수정: `bluetape4k/coroutines/README.ko.md`
- 외부 작업: #1297과 연결된 GitHub 후속 issue

- [x] **단계 1: 중복 issue를 read-only로 확인한다**

실행:

```bash
gh issue list --repo bluetape4k/bluetape4k-projects --state all --limit 100 \
  --search "delay error coroutines"
gh issue list --repo bluetape4k/bluetape4k-projects --state all --limit 100 \
  --search "backpressure overflow coroutines"
```

예상 결과: #1297과 동일한 delay-error/backpressure 후속 issue가 없다.

- [x] **단계 2: split follow-up issue를 생성하고 live metadata를 확인한다**

단계 1에서 중복을 찾지 못한 경우에만 영문 issue 하나를 생성한다.

```bash
gh issue create --repo bluetape4k/bluetape4k-projects \
  --title "enhancement(coroutines): evaluate delay-error and explicit overflow Flow policies" \
  --body $'Parent: #1297\n\n# Scope\nEvaluate delay-error composition and explicit overflow policies after the focused Flow parity slice.\n\n## Deferred families\n- concatDelayError, mergeDelayError, and bounded flatMapDelayError\n- onBackpressureBuffer, onBackpressureLatest, and explicit overflow errors\n- bufferWhen/windowWhen and bufferWhile/windowWhile when caller evidence exists\n\n## Constraints\n- Preserve CancellationException as cancellation.\n- Do not imply Reactive Streams demand semantics for Kotlin Flow.\n- Prefer standard kotlinx.coroutines operators when they already satisfy the contract.\n\n## Acceptance\n- Publish a contract matrix and caller evidence before implementation.\n- Add deterministic failure, cancellation, and bounded-memory tests.\n- Link the final PR back to #1297.'
```

이어서 `gh issue view`로 `state`, `assignees`, `milestone`, `url`을 검증한다.
Issue를 생성했다면 `gh issue edit <number> --assignee debop --milestone
"1.12.0"`을 실행하고 같은 field를 다시 읽는다. 중복 검색으로 찾은 기존
issue는 수정하지 않는다.

- [x] **단계 3: inventory matrix를 작성한다**

`docs/flow-operator-inventory.md`에는 다음 열과 행을 고정한다.

| 현재 API | 선택/제안 API | RxJava/Reactor 대응 | 표준 Flow 대응 또는 비목표 |
|---|---|---|---|
| `chunked`, `windowed` | `bufferTimeout`, `windowTimeout` | `Observable.buffer(timespan,count)`, `Flux.bufferTimeout` | 새 count-or-time 계약 |
| 없음 | `timeout`, `timeoutOrFallback` | `Observable.timeout`, `Flux.timeout` | 새 유휴 timeout 계약 |
| 무제한 `concatMapEager` | 제한된 overload | ordered eager concat 계열 | 사용자 정의 순서 보장 제한 mapping |
| 없음 | `switchMap` 용어 결정 | `switchMap`, `switchOnNext` | `flatMapLatest`. 이 issue에서는 wrapper를 추가하지 않음 |
| `merge`, `concat` | delay-error 후속 작업 | `mergeDelayError`, `concatDelayError` | 후속 issue |
| `onBackpressureDrop` | overflow 대응 | Reactor/Rx overflow 계열 | `buffer`/`conflate`. 후속 작업만 수행 |
| `withLatestFrom` | 기존 API | `withLatestFrom` | 기존 단일 secondary 사례 |
| 없음 | — | `combine`, `zip`, `retryWhen` | 표준 Flow 비목표 |

- [x] **단계 4: 양국어 README에 선택 범위와 표준 Flow 경계를 반영한다**

최종 signature로 compile되는 다음 예제를 추가한다.

```kotlin
val batches = source.bufferTimeout(maxSize = 100, timeout = 1.seconds)
val windows = source.windowTimeout(maxSize = 100, timeout = 1.seconds)
val recovered = source.timeoutOrFallback(500.milliseconds, fallback = cached)
val ordered = source.concatMapEager(maxConcurrency = 4, bufferCapacity = 8) { load(it) }
```

완료 시 비어 있지 않은 부분 batch/window를 방출하고, upstream 실패 시 처리 중인
부분 값을 폐기하며, timeout fallback은 upstream cancellation 뒤에 구독한다는
점을 문서화한다. 제외된 계열은 표준 Flow 또는 후속 issue에 대응시킨다.

- [x] **단계 5: inventory와 문서 diff를 검증하고 commit한다**

실행:

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

## 작업 2: Count-or-time buffer/window 구현(RED → GREEN)

**대상 파일:**

- 생성: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/bufferTimeout.kt`
- 생성: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/BufferTimeoutTest.kt`

- [x] **단계 1: 개수, timeout, 완료, 오류, cancellation, 가상 시간의 실패 테스트를 작성한다**

테스트 class는 기존 test fixture의 `runTest`, `assertResult`, `assertFailure`를
사용한다. 필수 case는 다음과 같다.

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

같은 시각의 경합 테스트가 문서화된 편향 select 규칙을 기록하도록 구현에서
수신 절을 `onTimeout`보다 먼저 등록해야 한다.

- [x] **단계 2: targeted RED run을 확인한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.BufferTimeoutTest' \
  --no-configuration-cache --console=plain
```

예상 결과: 새 API의 compile 오류 또는 symbol 부재로 실패한다.

- [x] **단계 3: 최소 구현을 작성한다**

내부 `countOrTimeout(maxSize, timeout): Flow<List<T>>` collector 하나를 사용해
`bufferTimeout`과 `windowTimeout`을 구현한다.

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

구현은 방출마다 새 list를 사용하고 비어 있지 않은 list만 방출해야 한다.
Channel 종료 원인은 그대로 전파하고 `windowTimeout`에서는 각 list를
`asFlow()`로 변환한다.

- [x] **단계 4: GREEN targeted run을 확인한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.BufferTimeoutTest' \
  --no-configuration-cache --console=plain
```

예상 결과: `BufferTimeoutTest`의 모든 case가 통과한다.

- [x] **단계 5: commit한다**

```bash
git add bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/bufferTimeout.kt \
  bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/BufferTimeoutTest.kt
git commit -m "add count-or-time Flow buffers and windows" \
  -m "Constraint: Timer behavior must remain deterministic under runTest virtual time.\nRejected: Reuse bufferingDebounce | it uses wall-clock System.nanoTime and has different reset semantics.\nConfidence: high\nScope-risk: moderate\nDirective: Preserve non-empty partial-on-completion and drop partial-on-failure semantics.\nTested: BufferTimeoutTest targeted Gradle run.\nNot-tested: Full module check is deferred to the slice gate."
```

## 작업 3: 유휴 timeout과 fallback 구현(RED → GREEN)

**대상 파일:**

- 생성: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/timeout.kt`
- 생성: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/TimeoutTest.kt`

- [x] **단계 1: 실패 테스트를 작성한다**

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

- [x] **단계 2: RED run을 확인한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.TimeoutTest' \
  --no-configuration-cache --console=plain
```

예상 결과: `FlowTimeoutException`/`timeout` symbol 부재로 실패한다.

- [x] **단계 3: 최소 구현을 작성한다**

공개 `FlowTimeoutException : java.util.concurrent.TimeoutException`
(`import java.util.concurrent.TimeoutException`)과 공개 함수 두 개를 구현한다.
내부 반복은 `onReceiveCatching`과 함께 `onTimeout(timeout)`을 등록한다. Timeout
발생 시 flag를 표시하고 명시적 upstream `Job`을 취소한 뒤 `cancelAndJoin`으로
종료를 기다린다. 이어서 input channel을 닫고 select 밖에서 예외를 던지거나
fallback을 수집한다. `CancellationException`은 다시 던지고 upstream/fallback
원인은 보존한다.

```kotlin
class FlowTimeoutException(val timeout: Duration) : TimeoutException(
    "Flow did not emit within $timeout",
)

fun <T> Flow<T>.timeout(timeout: Duration): Flow<T> =
    timeoutInternal(timeout, fallback = null)

fun <T> Flow<T>.timeoutOrFallback(timeout: Duration, fallback: Flow<T>): Flow<T> =
    timeoutInternal(timeout, fallback)
```

- [x] **단계 4: GREEN run을 확인하고 commit한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.TimeoutTest' \
  --no-configuration-cache --console=plain
git add bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/timeout.kt \
  bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/TimeoutTest.kt
git commit -m "add idle timeout and fallback Flow operators" \
  -m "Constraint: Upstream cancellation must complete before fallback collection.\nRejected: Expose TimeoutCancellationException as a data-plane error | cancellation would be indistinguishable from caller cancellation.\nConfidence: high\nScope-risk: moderate\nDirective: Always rethrow CancellationException and keep fallback single-subscription.\nTested: TimeoutTest targeted Gradle run.\nNot-tested: Full module check is deferred to the slice gate."
```

## 작업 4: 제한된 `concatMapEager` 구현(RED → GREEN)

**대상 파일:**

- 수정: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/concatMapEager.kt`
- 생성: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/ConcatMapEagerBoundedTest.kt`

- [x] **단계 1: 제한된 lifecycle 실패 테스트를 작성한다**

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

Transform 실패와 내부 실패가 원래 예외를 유지하는지 확인한다. `take(1)`
cancellation 뒤에는 시작한 모든 내부 작업이 `finally` 블록을 실행해야 한다.

- [x] **단계 2: RED run을 확인한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.ConcatMapEagerBoundedTest' \
  --no-configuration-cache --console=plain
```

예상 결과: 아직 제한된 overload가 없으므로 실패한다.

- [x] **단계 3: bounded overload와 channel-backed queue를 구현한다**

기존 함수를 변경하지 않고 다음 overload를 추가한다.

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

내부 queue는 `Semaphore(maxConcurrency)`와 `Channel<R>(bufferCapacity)`을
사용한다. 기존 동작을 바꾸지 않도록 기존 overload는
`ConcurrentLinkedQueue` 호환 경로를 유지하며 새 overload만 제한된 channel
경로를 사용한다. 내부 `finally`는 channel을 닫고 permit을 해제하며 완료를
표시한 뒤 drain을 재개한다. Child 실패를 catch한 뒤 무시해서는 안 된다.

- [x] **단계 4: GREEN run과 기존 회귀 테스트를 확인한다**

```bash
./gradlew :bluetape4k-coroutines:test --tests \
  'io.bluetape4k.coroutines.flow.extensions.ConcatMapEager*' \
  --no-configuration-cache --console=plain
```

예상 결과: 제한 동작 테스트와 기존 `ConcatMapEagerTest`가 통과한다.

- [x] **단계 5: commit한다**

```bash
git add bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/concatMapEager.kt \
  bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/ConcatMapEagerBoundedTest.kt
git commit -m "bound eager Flow mapping concurrency and queues" \
  -m "Constraint: Preserve the existing source-order overload and structured child lifecycle.\nRejected: Apply a global buffer after concatMapEager | it cannot bound per-inner queues while preserving ordered drain.\nConfidence: medium\nScope-risk: broad\nDirective: Keep semaphore permits and channel cleanup in inner finally blocks.\nTested: bounded and existing ConcatMapEager tests.\nNot-tested: Benchmark evidence is added in the next task."
```

## 작업 5: Benchmark와 양국어 API 문서화

**대상 파일:**

- 수정: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/benchmark/CoroutinesFlowBenchmark.kt`
- 수정: `bluetape4k/coroutines/README.md`
- 수정: `bluetape4k/coroutines/README.ko.md`
- 수정: `docs/flow-operator-inventory.md`

- [x] **단계 1: benchmark cases를 추가한다**

기존 `runBlocking`/`asFlow` 형식과
`import kotlin.time.Duration.Companion.days`를 사용해 benchmark method 세 개를
추가한다.

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

하루짜리 timeout은 benchmark가 wall-clock에 의존하지 않으면서 timer 등록과
list 할당을 측정하도록 의도한 값이다. 개수 경계와 제한된 queue 동작을 이번
배포의 안정적인 근거로 사용한다.

- [x] **단계 2: benchmark compile/run을 순차 실행한다**

```bash
./gradlew :bluetape4k-coroutines:testCoroutinesFlowBenchmark \
  --no-configuration-cache --console=plain
```

예상 결과: benchmark task가 완료되고 새 method 이름 세 개가 출력된다.

- [x] **단계 3: KDoc와 README parity를 마무리한다**

각 공개 API에 실행 가능한 예제와 명시적인 완료/오류/cancellation/순서/buffer/
concurrency 조항을 포함한 한국어 우선 KDoc을 작성한다. 영문 README와 한국어
README의 section 순서와 코드 signature를 동일하게 유지한다. 이 slice에는
시각적 계약 변경이 없으므로 diagram을 추가하지 않는다.

- [x] **단계 4: commit한다**

```bash
git add bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/benchmark/CoroutinesFlowBenchmark.kt \
  bluetape4k/coroutines/README.md bluetape4k/coroutines/README.ko.md docs/flow-operator-inventory.md
git commit -m "add Flow operator parity benchmarks and API guidance" \
  -m "Constraint: README locales and KDoc must describe the same public contracts.\nRejected: Add a new diagram for text-only operator contracts | existing diagrams do not encode timer/error details.\nConfidence: high\nScope-risk: narrow\nDirective: Update the inventory whenever a selected operator changes.\nTested: testCoroutinesFlowBenchmark and diff checks.\nNot-tested: External Rx/Reactor runtime integration is outside this module."
```

## 작업 6: 검증, 리뷰, lesson, stacked PR 제공

**대상 파일:**

- 생성: `docs/reviews/2026-08-03-issue-1297-flow-operator-review.md`
- 생성: `docs/lessons/2026-08-03-issue-1297-flow-operator-parity.md`

- [x] **단계 1: 전체 검증을 순차 실행한다**

```bash
./gradlew :bluetape4k-coroutines:test --no-configuration-cache --console=plain
./gradlew :bluetape4k-coroutines:check --no-configuration-cache --console=plain
./gradlew :bluetape4k-coroutines:testCoroutinesFlowBenchmark \
  --no-configuration-cache --console=plain
git diff --check
```

예상 결과: 모든 명령이 성공한다. 관련 없는 module까지 검증했다고 주장하지 않는다.

- [x] **단계 2: 최종 리뷰 문서를 작성한다**

설계와 branch diff를 대조하고 P0/P1/P2/P3 지적, API 호환성, 구조화된
cancellation, 가상 시간 결정성, queue 제한, README/KDoc 동등성, benchmark
출력, 후속 링크를 기록한다. P0=0, P1=0인 경우에만 다음 단계로 진행한다.

- [x] **단계 3: lesson을 작성하고 commit한다**

Timer 경합 결정, `bufferingDebounce`를 재사용하지 않은 이유, fallback 정리
근거, 제한된 queue의 trade-off를 정확한 명령과 함께 기록한다.

- [ ] **단계 4: PR train을 생성한다**

Feature branch를 push하고 dependency 순서대로 PR을 생성한다. 각 PR은 다음
조건을 충족해야 한다.

- 상황에 따라 `develop` 또는 바로 앞 PR head를 대상으로 한다.
- `debop`을 assignee로 지정하고 issue milestone/label을 복사하며 `#1297`을 연결한다.
- 영어 제목과 본문을 사용하고 본문은 `## DoD Status`로 끝낸다.
- 정확한 테스트 명령과 현재 commit SHA를 포함한다.

생성 전에 `git status`, `git log`, branch head, issue metadata를 검증한다.
최신 exact-head 승인이 없으면 이 작업에서 어떤 PR도 merge하지 않는다.

- [ ] **단계 5: merge-ready DoD를 보고하고 중단한다**

현재 최상위 head에서 `gh pr view`, `gh pr checks`, review/thread query,
mergeability 검사를 실행한다. 계획 항목 상태, 변경 파일, 필수 검사 `X/Y`, N/A와
blocked 수, 알려진 위험, 정확한 PR/head, 확인하지 않은 merge gate를 보고한다.
명시적 merge 승인을 기다리는 `PENDING` 상태에서 중단한다.

## 계획 자체 리뷰

- 명세 범위: 작업 1-5가 inventory, 선택한 API 계열 3개, 문서, 테스트,
  benchmark 근거, cancellation, 후속 작업 분리를 다룬다.
- Placeholder 검사: 해결되지 않은 placeholder나 지정되지 않은 검증 명령이 없다.
- 경계 검사: timer 시작, 같은 시각의 select 우선순위, cold window 반복 수집,
  잘못된 인자, cancellation marker에 정확한 테스트가 있다.
- Type 일관성: 모든 task에서 `Duration`, `FlowTimeoutException`,
  `timeoutOrFallback`, 제한된 `concatMapEager` signature를 일관되게 사용한다.
- 범위: 새 module/dependency가 없으며 모든 변경은 coroutines module, 문서,
  테스트, review/lesson 산출물 안에 있다.
