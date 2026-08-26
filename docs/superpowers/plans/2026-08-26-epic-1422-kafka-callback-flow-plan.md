# Epic #1422 #1347 Kafka CallbackFlow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 실제 Kafka producer callback을 `Flow<RecordMetadata>`로 변환하고 성공·실패·취소·backpressure·cleanup 계약을 실행 가능한 coroutines example로 증명한다.

**Architecture:** `CallbackFlowExamples.kt` 안의 private adapter만 Kafka callback lifecycle을 소유한다. 성공 경로는 기존 `KafkaServer.Launcher` broker helper를 사용하고, failure/lifecycle 경로는 producer factory seam을 사용하는 deterministic fixture로 분리한다. 이 parent child는 examples 공통 CI의 compile/test phase 분리와 diagnostics 수집도 소유하며, child #1353은 이 branch를 base로 쌓는다.

**Tech Stack:** Kotlin 2.4/JVM 25, kotlinx.coroutines `callbackFlow`/`awaitClose`, Apache Kafka 4 client, Testcontainers Kafka, JUnit 5, `runSuspendIO`, `bluetape4k-assertions`, existing Gradle version catalog.

**Approved basis:** `docs/superpowers/specs/2026-08-26-epic-1422-executable-examples-design.md` at commit `7d22431a975e12a237083c93d6e2e6749f966b9d`, based on `origin/develop` `a907d144f39bfb94cba783cf65a5412e0714e9d5`.

---

## 계획 범위와 파일 소유권

- Modify: `examples/coroutines-demo/build.gradle.kts` — 기존 catalog/project를 재사용하는 test dependency만 추가한다.
- Modify: `examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/flow/CallbackFlowExamples.kt` — private adapter, deterministic fixture, actual broker test를 한 example class에 둔다.
- Modify: `examples/coroutines-demo/README.md` — 정확한 Gradle task, Docker/Testcontainers precondition, callback contract를 영어로 갱신한다.
- Modify: `examples/coroutines-demo/README.ko.md` — 같은 명령과 계약을 한국어로 갱신한다.
- Create: `.github/scripts/collect-testcontainers-diagnostics.py` — task별 bounded/sanitized Docker log와 provenance manifest를 표준 출력 경로에 만든다.
- Create: `.github/scripts/test_collect_testcontainers_diagnostics.py` — redaction·allowlist·report cap 회귀 fixture를 검증한다.
- Modify: `.github/workflows/examples.yml` — compile 병렬 phase와 Testcontainers test 순차 phase를 분리하고 aggregate failure/artifact를 고정한다.

이 계획은 production Kafka API, 새 library module, catalog version, 공용 Kafka image/tag를
변경하지 않는다. CI/test-only broker image는 immutable digest로 고정한다. #1353 소유
파일은 이 계획에서 수정하지 않는다.

## Task 1: Kafka example dependency와 baseline 고정

**Files:**

- Modify: `examples/coroutines-demo/build.gradle.kts`
- Test: `examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/flow/CallbackFlowExamples.kt`

- [ ] **Step 1: 기존 dependency 블록에 명시적 test project를 추가한다**

`dependencies` 안의 `testImplementation(project(":bluetape4k-junit5"))` 앞에 다음 세 줄을 추가한다. catalog의 기존 alias와 project path만 사용한다.

```kotlin
    testImplementation(project(":bluetape4k-kafka4"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers.kafka)
```

- [ ] **Step 2: dependency와 기존 test source가 함께 컴파일되는지 확인한다**

Run:

```bash
./gradlew :bluetape4k-examples-coroutines-demo:testClasses --no-configuration-cache
```

Expected: `BUILD SUCCESSFUL`; Kafka client와 `KafkaServer.Launcher`를 resolve하고 기존 `CallbackFlowExamples` test source를 컴파일한다.

- [ ] **Step 3: 변경을 독립 commit으로 기록한다**

```bash
git add examples/coroutines-demo/build.gradle.kts
git commit -F - <<'EOF'
Kafka callback 예제의 기존 broker·client helper 연결을 보장한다

Constraint: 새로운 version coordinate나 production dependency를 추가하지 않는다.
Rejected: compileOnly Kafka dependency | test runtime에서 실제 producer를 만들 수 없다.
Confidence: high
Scope-risk: narrow
Directive: 다음 Kafka example은 KafkaServer.Launcher와 중앙 catalog만 재사용한다.
Tested: ./gradlew :bluetape4k-examples-coroutines-demo:testClasses --no-configuration-cache
Not-tested: callback lifecycle, broker runtime, hosted CI
EOF
```

## Task 2: deterministic lifecycle RED 테스트 작성

**Files:**

- Modify: `examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/flow/CallbackFlowExamples.kt`

- [ ] **Step 1: test fixture와 결과 assertion을 추가한다**

`CallbackFlowExamples` 안에 `runSuspendIO`와 `bluetape4k-assertions`를 사용해 다음 행동을 각각 검증한다. 테스트 이름과 기대 예외를 그대로 유지한다.

```kotlin
@Test
fun `callback failure preserves first cause and closes producer once`() = runSuspendIO {
    val failure = IllegalStateException("callback failure")
    val producer = TrackingProducer(callbackError = failure)

    val error = assertFailsWith<IllegalStateException> {
        producerResults(flowOf(record("failure")), { producer.producer }).toList()
    }

    error shouldBeEqualTo failure
    producer.closeCount.get() shouldBeEqualTo 1
    producer.callbackCount.get() shouldBeEqualTo 1
}

@Test
fun `backpressure fails without dropping callback and closes producer once`() = runSuspendIO {
    val producer = TrackingProducer(holdCallbacks = true)
    val collectorGate = CompletableDeferred<Unit>()
    val firstItemReceived = CompletableDeferred<Unit>()
    val collection = async {
        var received = 0
        producerResults(
            records = flowOf(record("one"), record("two"), record("three")),
            producerFactory = { producer.producer },
            channelCapacity = 1,
            maxInFlight = 2,
        ).collect {
            val index = received
            received += 1
            if (index == 0) {
                firstItemReceived.complete(Unit)
                collectorGate.await()
            }
        }
    }

    withTimeout(5.seconds) { producer.twoSendsStarted.await() }
    producer.pendingSends.distinct().size shouldBeEqualTo 2
    producer.fireCallback()
    withTimeout(5.seconds) { firstItemReceived.await() }
    withTimeout(5.seconds) { producer.allSendsStarted.await() }
    producer.pendingSends.distinct().size shouldBeEqualTo 3
    producer.fireCallback()
    producer.fireCallback()
    collectorGate.complete(Unit)
    val error = assertFailsWith<IllegalStateException> {
        withTimeout(10.seconds) { collection.await() }
    }

    error.message shouldBeEqualTo "callback buffer is full"
    producer.closeCount.get() shouldBeEqualTo 1
    producer.callbackCount.get() shouldBeEqualTo 3
}

@Test
fun `in-flight permit is released after callback`() = runSuspendIO {
    val producer = TrackingProducer()

    producerResults(
        records = flowOf(record("permit-one"), record("permit-two")),
        producerFactory = { producer.producer },
        channelCapacity = 2,
        maxInFlight = 1,
    ).toList()

    producer.callbackCount.get() shouldBeEqualTo 2
}

@Test
fun `collector cancellation rethrows CancellationException and closes producer once`() = runSuspendIO {
    val producer = TrackingProducer(holdCallbacks = true)
    val task = async {
        producerResults(flowOf(record("cancel")), { producer.producer }).toList()
    }

    withTimeout(5.seconds) { producer.sendStarted.await() }
    task.cancel()
    withTimeout(5.seconds) {
        while (producer.cancelledPendingSends() == 0) delay(10)
    }
    val cancellation = assertFailsWith<CancellationException> { task.await() }

    producer.closeCount.get() shouldBeEqualTo 1
    producer.pendingSend.isCancelled shouldBeEqualTo true
    producer.cancelledPendingSends() shouldBeEqualTo 1
    producer.fireLateCallback()
    producer.callbackCount.get() shouldBeEqualTo 0
    producer.lateCallbackCount.get() shouldBeEqualTo 1
    val afterLate = assertFailsWith<CancellationException> { task.await() }
    afterLate::class shouldBeEqualTo cancellation::class
    afterLate.message shouldBeEqualTo cancellation.message
}

@Test
fun `cancellation immediately after producer creation closes it once`() = runSuspendIO {
    val producer = TrackingProducer(holdCallbacks = true)
    val factoryStarted = CompletableDeferred<Unit>()
    val task = async {
        producerResults(
            flowOf(record("immediate-cancel")),
            {
                factoryStarted.complete(Unit)
                producer.producer
            },
        ).toList()
    }

    withTimeout(5.seconds) { factoryStarted.await() }
    task.cancel()
    assertFailsWith<CancellationException> { task.await() }

    producer.closeCount.get() shouldBeEqualTo 1
}

@Test
fun `factory and synchronous send failures are terminal causes`() = runSuspendIO {
    val factoryFailure = IllegalArgumentException("factory failure")
    val factoryError = assertFailsWith<IllegalArgumentException> {
        producerResults(flowOf(record("factory")), { throw factoryFailure }).toList()
    }
    factoryError shouldBeEqualTo factoryFailure

    val sendFailure = IllegalStateException("send failure")
    val sendProducer = TrackingProducer(sendError = sendFailure)
    val sendError = assertFailsWith<IllegalStateException> {
        producerResults(flowOf(record("send")), { sendProducer.producer }).toList()
    }
    sendError shouldBeEqualTo sendFailure
    sendProducer.closeCount shouldBeEqualTo 1
}

@Test
fun `normal completion drains callbacks flushes and closes once`() = runSuspendIO {
    val producer = TrackingProducer()

    producerResults(flowOf(record("normal")), { producer.producer }).toList()

    producer.flushCount.get() shouldBeEqualTo 1
    producer.closeCount.get() shouldBeEqualTo 1
    producer.callbackCount.get() shouldBeEqualTo 1
}

@Test
fun `mixed synchronous and asynchronous callbacks drain before close`() = runSuspendIO {
    val producer = TrackingProducer(heldSendIndexes = setOf(2))
    val collection = async {
        producerResults(
            flowOf(record("sync"), record("async")),
            { producer.producer },
        ).toList()
    }

    withTimeout(5.seconds) { producer.twoSendsStarted.await() }
    producer.fireCallback()
    collection.await()

    producer.callbackCount.get() shouldBeEqualTo 2
    producer.flushCount.get() shouldBeEqualTo 1
    producer.closeCount.get() shouldBeEqualTo 1
}

@Test
fun `flush failure becomes the terminal cause after callback drain`() = runSuspendIO {
    val flushFailure = IllegalStateException("flush failure")
    val producer = TrackingProducer(flushError = flushFailure)

    val error = assertFailsWith<IllegalStateException> {
        producerResults(flowOf(record("flush-failure")), { producer.producer }).toList()
    }

    error shouldBeEqualTo flushFailure
    producer.closeCount.get() shouldBeEqualTo 1
}

@Test
fun `upstream exception remains primary and cleanup failure is suppressed`() = runSuspendIO {
    val upstreamFailure = IllegalArgumentException("upstream failure")
    val producer = TrackingProducer(closeError = IllegalStateException("close failure"))

    val error = assertFailsWith<IllegalArgumentException> {
        producerResults(
            flow {
                emit(record("before-upstream-failure"))
                throw upstreamFailure
            },
            { producer.producer },
        ).toList()
    }

    error shouldBeEqualTo upstreamFailure
    error.suppressed.single() shouldBeEqualTo producer.closeError!!
    producer.closeCount.get() shouldBeEqualTo 1
}

@Test
fun `channel and in flight bounds reject invalid values`() = runSuspendIO {
    assertFailsWith<IllegalArgumentException> {
        producerResults(flowOf(record("invalid-capacity")), { TrackingProducer().producer }, channelCapacity = 0).toList()
    }
    assertFailsWith<IllegalArgumentException> {
        producerResults(flowOf(record("invalid-in-flight")), { TrackingProducer().producer }, maxInFlight = 17).toList()
    }
}

@Test
fun `callback drain timeout cancels pending sends and preserves timeout cause`() = runSuspendIO(timeout = 40.seconds) {
    val producer = TrackingProducer(holdCallbacks = true)

    val error = assertFailsWith<TimeoutCancellationException> {
        producerResults(flowOf(record("timeout")), { producer.producer }).toList()
    }

    error::class shouldBeEqualTo TimeoutCancellationException::class
    producer.closeCount.get() shouldBeEqualTo 1
    producer.pendingSend.isCancelled shouldBeEqualTo true
    producer.cancelledPendingSends() shouldBeEqualTo 1
}
```

`record(value)`는 다음 private helper로 unique topic을 만들고 topic/key/value/header 길이 계약을 검사한다.
`org.apache.kafka.common.header.internals.RecordHeaders`를 import해 고정된 비밀정보 없는
header를 하나만 부착한다.

```kotlin
private fun record(value: String): ProducerRecord<String, String> {
    val topic = "epic-1422-${Base58.randomString(8)}"
    val key = "key"
    val headerName = "x-epic-1422"
    val headerValue = "example"
    require(topic.length <= 128)
    require(key.length <= 128)
    require(value.toByteArray(Charsets.UTF_8).size <= 1024)
    require(headerName.toByteArray(Charsets.UTF_8).size <= 256)
    require(headerValue.toByteArray(Charsets.UTF_8).size <= 256)
    val headers = RecordHeaders().add(headerName, headerValue.toByteArray(Charsets.UTF_8))
    return ProducerRecord(topic, null, null, key, value, headers)
}
```

`TrackingProducer`는 MockK로 감싼 test-only `Producer<String, String>` probe다. 다음
설정을 생성자에서 받고, 테스트 factory가 `producer`를 반환하도록 고정한다.

```kotlin
private class TrackingProducer(
    private val callbackError: Exception? = null,
    private val sendError: Exception? = null,
    private val flushError: Exception? = null,
    val closeError: Exception? = null,
    private val holdCallbacks: Boolean = false,
    private val heldSendIndexes: Set<Int> = emptySet(),
) {
    val producer: Producer<String, String> = mockk(relaxed = true)
    val callbackCount = AtomicInteger()
    val closeCount = AtomicInteger()
    val flushCount = AtomicInteger()
    val lateCallbackCount = AtomicInteger()
    val sendStarted = CompletableDeferred<Unit>()
    val twoSendsStarted = CompletableDeferred<Unit>()
    val allSendsStarted = CompletableDeferred<Unit>()
    val sendCount = AtomicInteger()
    data class Pending(val callback: Callback, val future: CompletableFuture<RecordMetadata>)
    val pendingCallbacks = ConcurrentLinkedQueue<Pending>()
    val pendingSends = ConcurrentLinkedQueue<CompletableFuture<RecordMetadata>>()
    val pendingSend: CompletableFuture<RecordMetadata>
        get() = pendingSends.peek() ?: error("no pending Kafka send")
    private val closed = AtomicBoolean()
    private val metadata = mockk<RecordMetadata>(relaxed = true)

    init {
        every { producer.send(any<ProducerRecord<String, String>>(), any()) } answers {
            if (sendError != null) throw sendError
            val callback = secondArg<Callback>()
            val index = sendCount.incrementAndGet()
            val hold = holdCallbacks || index in heldSendIndexes
            val resultFuture = if (hold) {
                val future = CompletableFuture<RecordMetadata>()
                pendingSends += future
                pendingCallbacks += Pending(callback, future)
                sendStarted.complete(Unit)
                future
            } else {
                callbackCount.incrementAndGet()
                callback.onCompletion(metadata, callbackError)
                sendStarted.complete(Unit)
                CompletableFuture.completedFuture(metadata)
            }
            when (index) {
                2 -> twoSendsStarted.complete(Unit)
                3 -> allSendsStarted.complete(Unit)
            }
            resultFuture
        }
        every { producer.flush() } answers {
            flushCount.incrementAndGet()
            flushError?.let { throw it }
        }
        every { producer.close(any<Duration>()) } answers {
            closeCount.incrementAndGet()
            closed.set(true)
            closeError?.let { throw it }
        }
    }

    fun fireLateCallback() {
        pendingCallbacks.poll()?.let { pending ->
            if (closed.get()) lateCallbackCount.incrementAndGet()
            pending.callback.onCompletion(metadata, null)
            pending.future.complete(metadata)
        }
    }

    fun fireCallback() {
        pendingCallbacks.poll()?.let { pending ->
            callbackCount.incrementAndGet()
            pending.callback.onCompletion(metadata, null)
            pending.future.complete(metadata)
        }
    }

    fun cancelledPendingSends(): Int = pendingSends.count { it.isCancelled }
}
```

기본 성공 fixture는 callback을 즉시 호출하고, held fixture는 callback과 매번 새로
생성한 `CompletableFuture`를 `Pending` 쌍으로 보관해 drain timeout과 cancellation을
결정적으로 만든다. backpressure 테스트는 `maxInFlight=2`에서 처음 두 `send`를
`twoSendsStarted`로 확인한 뒤 첫 callback을 발화한다. 첫 callback 수신 후 collector가
gate에서 멈추고 permit이 반환되면 세 번째 send가 시작된다. `allSendsStarted`를
확인한 뒤 두 번째·세 번째 callback을 순서대로 발화하면 두 번째 결과가 1-slot
buffer를 채우고 세 번째 callback이 full을 재현한다. `maxInFlight=1`에서
두 record가 모두 완료되는 테스트가 callback 공통 `finally`의 permit release를
간접적으로 증명한다. close 이후 callback은 `lateCallbackCount`만 증가시킨다.
`heldSendIndexes`를 사용하는 mixed 테스트는 첫 send의 synchronous callback과 두 번째
send의 asynchronous callback을 함께 실행해 마지막 callback 처리 전에 flush/close가
시작되지 않음을 검증한다.
취소 테스트는 late callback 뒤에도 동일한 `CancellationException`이 다시 관찰되고
추가 결과가 발행되지 않음을 확인해 terminal state 불변성을 검증한다.
각 held send는 서로 다른 Future를 반환하며 `cancelledPendingSends()`로 다중 in-flight
취소를 검증할 수 있다. `Producer`의 다른 메서드는 MockK relaxed
기본값으로 두되 `send`, `flush`, `close(Duration)`만 위 계약으로 검증한다. 모든
assertion은 `assertFailsWith`, `shouldBeEqualTo` 또는 기존 `bluetape4k` matcher를
사용한다. Timeout 테스트는 adapter의 30초 drain deadline을 그대로 검증하므로
바깥 `runSuspendIO`는 40초로 제한한다.

- [ ] **Step 2: RED 상태를 확인한다**

Run:

```bash
./gradlew :bluetape4k-examples-coroutines-demo:test \
  --tests 'io.bluetape4k.examples.coroutines.flow.CallbackFlowExamples' \
  --no-configuration-cache
```

Expected: 새 `producerResults`와 `TrackingProducer`가 아직 없으므로 compile/test가 실패한다. failure가 test discovery 또는 missing symbol인지 확인하고 lifecycle 실패가 아닌 다른 환경 오류는 기록한다.

## Task 3: private callbackFlow adapter를 구현한다

**Files:**

- Modify: `examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/flow/CallbackFlowExamples.kt`

- [ ] **Step 1: public surface 없이 adapter의 정확한 signature를 정의한다**

```kotlin
private fun producerResults(
    records: Flow<ProducerRecord<String, String>>,
    producerFactory: () -> Producer<String, String>,
    channelCapacity: Int = 16,
    maxInFlight: Int = 16,
): Flow<RecordMetadata>
```

Step 2–3에서 이 signature의 함수 body를 완성한다. `channelCapacity`와
`maxInFlight`를 각각 `1..16`으로 검증하고, 마지막에
`.buffer(channelCapacity, onBufferOverflow = BufferOverflow.SUSPEND)`를 적용한다.
필요한 coroutine import는 `CancellationException`, `CoroutineStart`, `Dispatchers`,
`Job`, `NonCancellable`, `TimeoutCancellationException`, `delay`, `launch`,
`runInterruptible`, `withContext`, `withTimeout`이며, callback state에는
`AtomicBoolean`, `AtomicReference`, `ConcurrentHashMap`,
`kotlinx.coroutines.sync.Semaphore`를 사용한다. producer 생성은 LAZY worker 내부의
`try/finally` 소유 범위에서 수행해 worker가 시작되기 전에 downstream이 취소되면
producer를 만들지 않고, 생성된 뒤에는 worker가 반드시 close한다.
`callbackFlow` 내부는 다음 이름과 수명 순서를 그대로 사용한다.

```kotlin
return callbackFlow {
    val terminalCause = AtomicReference<Throwable?>(null)
    val downstreamCancelled = AtomicBoolean()
    val permits = Semaphore(maxInFlight)
    class SendState {
        val future = AtomicReference<Future<RecordMetadata>?>(null)
        val completed = AtomicBoolean()
    }
    val inFlight = ConcurrentHashMap.newKeySet<SendState>()
    val upstreamJobRef = AtomicReference<Job?>()

    fun cancelInFlight() {
        inFlight.toList().forEach { state ->
            state.future.get()?.cancel(false)
            if (state.completed.compareAndSet(false, true)) {
                inFlight.remove(state)
                permits.release()
            }
        }
    }

    fun failOnce(cause: Throwable) {
        if (downstreamCancelled.get()) return
        if (terminalCause.compareAndSet(null, cause)) {
            cancelInFlight()
            upstreamJobRef.get()?.cancel(CancellationException("producer terminal failure", cause))
            close(cause)
        } else {
            log.debug { "late Kafka callback ignored; failureKind=${cause::class.simpleName}" }
        }
    }

    fun complete(state: SendState, metadata: RecordMetadata?, cause: Exception?) {
        if (!state.completed.compareAndSet(false, true)) return
        try {
            if (cause != null) {
                failOnce(cause)
            } else if (metadata != null) {
                val result = trySend(metadata)
                if (result.isFailure && !result.isClosed && !downstreamCancelled.get()) {
                    failOnce(IllegalStateException("callback buffer is full"))
                }
            }
        } finally {
            inFlight.remove(state)
            permits.release()
        }
    }

    fun callbackFor(state: SendState): Callback = Callback { metadata, cause ->
        complete(state, metadata, cause)
    }

    val upstreamJob = launch(context = Dispatchers.IO, start = CoroutineStart.LAZY) {
        var producer: Producer<String, String>? = null
        try {
            val activeProducer = producerFactory()
            producer = activeProducer
            records.collect { record ->
                permits.acquire()
                val state = SendState()
                inFlight += state
                try {
                    val future = activeProducer.send(record, callbackFor(state))
                    state.future.set(future)
                    if (terminalCause.get() != null || downstreamCancelled.get()) {
                        future.cancel(false)
                    }
                } catch (cause: CancellationException) {
                    if (state.completed.compareAndSet(false, true)) {
                        inFlight.remove(state)
                        permits.release()
                    }
                    if (!downstreamCancelled.get()) failOnce(cause)
                    throw cause
                } catch (cause: Throwable) {
                    if (state.completed.compareAndSet(false, true)) {
                        inFlight.remove(state)
                        permits.release()
                    }
                    failOnce(cause)
                    throw cause
                }
            }
        } catch (cause: CancellationException) {
            if (!downstreamCancelled.get()) failOnce(cause)
            throw cause
        } catch (cause: Throwable) {
            failOnce(cause)
            throw cause
        } finally {
            producer?.let { activeProducer ->
                withContext(NonCancellable + Dispatchers.IO) {
                    var cleanupFailure: Throwable? = null
                    var cleanupCancellation: CancellationException? = null
                    try {
                        withTimeout(30.seconds) {
                            while (inFlight.isNotEmpty()) delay(10)
                            runInterruptible { activeProducer.flush() }
                        }
                    } catch (cause: TimeoutCancellationException) {
                        cleanupFailure = cause
                        cancelInFlight()
                        if (!downstreamCancelled.get()) failOnce(cause)
                    } catch (cause: CancellationException) {
                        cleanupFailure = cause
                        cleanupCancellation = cause
                        cancelInFlight()
                        if (!downstreamCancelled.get()) failOnce(cause)
                    } catch (cause: Throwable) {
                        cleanupFailure = cause
                        cancelInFlight()
                        if (!downstreamCancelled.get()) failOnce(cause)
                    }

                    val closeFailure = runCatching {
                        withTimeout(5.seconds) {
                            runInterruptible { activeProducer.close(Duration.ofSeconds(5)) }
                        }
                    }.exceptionOrNull()
                    val first = terminalCause.get()
                    if (first != null && cleanupFailure != null && first !== cleanupFailure) {
                        first.addSuppressed(cleanupFailure)
                    }
                    if (first != null && closeFailure != null && first !== closeFailure) {
                        first.addSuppressed(closeFailure)
                    }
                    if (first == null && closeFailure != null && !downstreamCancelled.get()) {
                        failOnce(closeFailure)
                    }
                    if (terminalCause.get() == null && !downstreamCancelled.get()) close()
                    if (cleanupCancellation != null && terminalCause.get() == null) {
                        throw cleanupCancellation
                    }
                }
            }
        }
    }
    upstreamJobRef.set(upstreamJob)
    upstreamJob.start()
    awaitClose {
        downstreamCancelled.set(true)
        cancelInFlight()
        upstreamJob.cancel(CancellationException("collector cancelled"))
    }
}.buffer(channelCapacity, onBufferOverflow = BufferOverflow.SUSPEND)
```

각 send는 `SendState`를 먼저 `inFlight`에 등록하고 callback에 전달한다. callback이
동기 실행되어도 state가 이미 존재하므로 stale Future가 생기지 않는다. callback은
결과 전달 또는 `failOnce` 처리가 끝난 뒤 `finally`에서 state를 `inFlight`에서 제거하고
permit을 정확히 한 번 해제한다. `cancelInFlight()`도 state의 completed CAS를 통해
같은 제거·permit release를 한 번만 수행한다. worker cleanup은 one-shot signal에 의존하지 않고
bounded loop에서 `inFlight.isEmpty()`를 다시 확인하므로 mixed sync/async callback이
남아 있으면 flush/close로 진행하지 않는다. 반환 Future는 state에
저장한 뒤 이미 완료된 callback이면 registry에 남기지 않는다. `awaitClose`는 먼저
`downstreamCancelled` sentinel을 세우고 in-flight state를 완료·제거하면서
non-blocking Future cancellation과 cancellation signal을 전달한다. 따라서 닫힌
channel에서 도착한 late callback은 새 terminal cause나 permit release를 만들지 않는다.
`failOnce(cause)`는 downstream sentinel이 없을 때만
`terminalCause`를 CAS한 뒤 `upstreamJobRef`가 가리키는 Job을 취소하고 channel close를
수행한다. Job은 `CoroutineStart.LAZY`로 만든 뒤 ref를 저장하고 시작하므로 callback이
동기 실행되어도 초기화되지 않은 Job을 참조하지 않는다. 각 collection마다
`producerFactory()`는 LAZY worker가 실제로 시작할 때 한 번만 호출하며 worker의
`try/finally`가 생성된 producer의 소유자가 된다. 두 helper는 `upstreamJob`을 생성하기 전에 같은 `callbackFlow` block의
local function으로 선언하며, callback thread에서 suspend하지 않는다.
각 `send`의 반환 Future는 state에 저장하고, cancellation 또는 drain deadline에서
모든 in-flight state의 Future를 `cancel(false)`한다. deterministic probe는 send마다
서로 다른 pending Future를 반환하고 `pendingSend`와 `cancelledPendingSends()`로 그
결과를 검증한다. Future를 state에 저장한 직후 `downstreamCancelled`도 확인하므로
`sendStarted`와 Future 등록 사이에 collector가 취소되는 경합도 놓치지 않는다.

- [ ] **Step 2: callback failure와 full-buffer terminal path를 구현한다**

첫 terminal cause는 다음 규칙을 사용한다. Step 1의 local helper와 동일하게
`upstreamJobRef`를 사용해 초기화 전 Job 참조를 막고, downstream cancellation 뒤에는
late callback이 새 원인을 만들지 않는다.

```kotlin
if (!downstreamCancelled.get() && terminalCause.compareAndSet(null, cause)) {
    upstreamJobRef.get()?.cancel(CancellationException("producer terminal failure", cause))
    close(cause)
} else {
    log.debug { "late Kafka callback ignored; failureKind=$failureKind" }
}
```

`trySend`가 full이면서 channel이 아직 닫히지 않은 경우에만
`IllegalStateException("callback buffer is full")`을 위 CAS에 넣고 worker/upstream을
취소한다. `ChannelResult.isClosed` 또는 `downstreamCancelled`인 경우는 정상적인
종료/취소로 분류해 새 cause를 만들지 않는다. callbackFlow channel과 producer close는
각각 한 번만 실행하며, cleanup 예외는 first cause의 `suppressed`에 붙인다.

- [ ] **Step 3: awaitClose와 bounded cleanup을 구현한다**

`awaitClose`에서는 sentinel 설정, in-flight Future의 non-blocking `cancel(false)`,
cancellation signal만 수행하고 blocking Kafka API를 호출하지 않는다. worker의
`finally` 안에서 다음 순서를 지킨다.

1. `withContext(NonCancellable + Dispatchers.IO)`로 진입한다.
2. callback drain과 `flush()`를 30초 `withTimeout`으로 기다린다.
   `while (inFlight.isNotEmpty()) delay(10)` bounded loop를 사용해 모든 callback
   처리가 끝난 뒤에만 flush/close로 진행한다. 모든 send가 callback 완료 후에만
   정상 close로 간다.
3. deadline을 넘기면 `TimeoutCancellationException`을 first cause로 CAS하고 pending send를 취소한다.
   cleanup 중 발생한 `CancellationException`은 broad `Throwable` catch로 흡수하지 않고
   cleanup 후 재전파하며, 기존 upstream/downstream cancellation 원인은 그대로 보존한다.
4. `producer.close(Duration.ofSeconds(5))`를 한 번 실행하고 예외를 suppressed로 연결한다.
5. `CancellationException`을 broad catch에서 삼키지 않고 재전파한다. close 자체의
   후속 예외는 기존 cancellation 또는 first cause에 `suppressed`로만 연결한다.

정상적인 `records.collect` 완료에서는 drain과 flush가 끝난 뒤 channel을 성공적으로
닫고, terminal cause가 있으면 해당 cause로 이미 닫힌 channel을 유지한다. drain 또는
flush/close 중 발생한 후속 예외는 first cause에만 `suppressed`로 연결한다. pending
state의 Future는 close 호출 전에 모두 `cancel(false)`해 callback이 뒤늦게 도착해도
새 결과를 발행하지 않도록 한다.

`runBlocking`이나 IO dispatcher 밖의 blocking Kafka call을 추가하지 않는다.

- [ ] **Step 4: RED 테스트를 GREEN으로 전환한다**

Run:

```bash
./gradlew :bluetape4k-examples-coroutines-demo:test \
  --tests 'io.bluetape4k.examples.coroutines.flow.CallbackFlowExamples' \
  --no-configuration-cache
```

Expected: Task 2의 failure/lifecycle tests가 PASS하고, close count=1, first cause identity, callback/permit count가 assertion된다.

## Task 4: 실제 Kafka broker 성공 경로와 consumer 검증을 추가한다

**Files:**

- Modify: `examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/flow/CallbackFlowExamples.kt`

- [ ] **Step 1: digest-pinned Testcontainers precondition과 실제 fixture를 사용한다**

`runSuspendIO(timeout = 120.seconds)`에서 다음 immutable image reference로 test-owned
broker를 하나 시작하고 `ShutdownQueue`에 등록한다. `DockerImageName`과 기존
`KafkaServer.Launcher.createStringProducer(broker)`/`createStringConsumer(broker)`를
그대로 재사용해 새 client factory를 만들지 않는다.
필요한 import는 `org.testcontainers.utility.DockerImageName`와
`io.bluetape4k.utils.ShutdownQueue`다.

```kotlin
private const val KAFKA_IMAGE_REF =
    "confluentinc/cp-kafka@sha256:a5040785528b0bce3b146febe9fcacdcf2b9b5acb450307f75170ef0e60ec130"

val broker = KafkaServer(DockerImageName.parse(KAFKA_IMAGE_REF)).apply {
    start()
    ShutdownQueue.register(this)
}
```

topic은 `"epic-1422-callback-" + Base58.randomString(8)`로 만들고, record 수는 128개
이하, value는 1 KiB 이하로 제한한다. producer와 consumer가 broker 자체를 종료하지
않고 collection-scoped/test-owned resource만 닫도록 `use`와 bounded close를 적용한다.

- [ ] **Step 2: success cardinality와 eventual broker result를 검증한다**

```kotlin
@Test
fun `real Kafka producer callbacks become metadata flow`() = runSuspendIO(timeout = 120.seconds) {
    val topic = "epic-1422-callback-${Base58.randomString(8)}"
    val records = (0 until 8).map { index ->
        ProducerRecord(topic, "key-$index", "value-$index")
    }

    val broker = KafkaServer(DockerImageName.parse(KAFKA_IMAGE_REF)).apply {
        start()
        ShutdownQueue.register(this)
    }
    val consumer = KafkaServer.Launcher.createStringConsumer(broker)
    try {
        consumer.subscribe(listOf(topic))
        val metadata = producerResults(
            records = records.asFlow(),
            producerFactory = { KafkaServer.Launcher.createStringProducer(broker) },
        ).toList()

        metadata shouldHaveSize records.size
        val polled = withTimeout(10.seconds) {
            generateSequence { consumer.poll(Duration.ofMillis(250)).toList() }
                .flatten()
                .take(records.size)
                .toList()
        }
        polled shouldHaveSize records.size
    } finally {
        withContext(NonCancellable + Dispatchers.IO) {
            withTimeout(5.seconds) {
                consumer.close(Duration.ofSeconds(5))
            }
        }
    }
}
```

metadata 순서는 `maxInFlight` 때문에 비교하지 않고 cardinality와 실제 consumer record count만 비교한다. consumer poll은 10초를 넘기지 않는다.

- [ ] **Step 3: actual broker test를 순차적으로 실행한다**

Run:

```bash
./gradlew :bluetape4k-examples-coroutines-demo:test \
  --tests 'io.bluetape4k.examples.coroutines.flow.CallbackFlowExamples' \
  --no-configuration-cache --max-workers=1
```

Expected: Docker/Testcontainers가 동작하는 환경에서 deterministic tests와 broker success test가 모두 PASS한다. Docker daemon이 없으면 코드 실패로 분류하지 않고 container startup evidence를 기록한다.

## Task 5: bounded diagnostics와 examples CI test phase를 분리한다

**Files:**

- Create: `.github/scripts/collect-testcontainers-diagnostics.py`
- Modify: `.github/workflows/examples.yml`

- [ ] **Step 1: diagnostics collector를 추가한다**

Python 표준 라이브러리만 사용해 다음 CLI 계약을 구현한다.

```text
python3 .github/scripts/collect-testcontainers-diagnostics.py \
  --task-name :bluetape4k-examples-coroutines-demo:test \
  --output-dir examples/build/testcontainers-diagnostics/_bluetape4k-examples-coroutines-demo_test \
  --workflow-file .github/workflows/examples.yml \
  --max-total-bytes 2000000 \
  --container-id <new-container-id>

python3 .github/scripts/collect-testcontainers-diagnostics.py \
  --task-name examples-test-phase-reports \
  --output-dir examples/build/testcontainers-diagnostics/report-scan \
  --workflow-file .github/workflows/examples.yml \
  --sanitized-report-dir examples/build/sanitized-test-reports \
  --report-path examples/coroutines-demo/build/test-results/test \
  --report-path examples/coroutines-demo/build/reports/tests/test \
  --max-report-files 200 \
  --max-report-total-bytes 2000000
```

스크립트는 `--container-id`를 반복 인자로 받아 현재 Gradle invocation에서 새로 생성된
container만 대상으로 삼는다. ID가 없으면 container 섹션이 빈 manifest가 되며 exit 0으로
처리하되, `--report-path`가 함께 지정된 경우 report sanitization은 계속 수행한다.
각 container에 대해 `docker inspect`의 image/name/created와 `docker logs --tail 200`만
수집한다. 출력은 task 이름으로 정규화한 JSON manifest와 `*.log`로 저장하며,
token·URI·환경 변수·payload·exception message는 `[REDACTED]`로 치환한다. 로그와
manifest의 총 산출량은 `--max-total-bytes`(기본 2,000,000 bytes)를 넘지 않으며,
초과분은 잘라내고 manifest에 `truncated=true`를 기록한다. container log가 없으면
해당 container의 로그를 생략하고, 전체 ID가 비어 있으면 빈 manifest를 만든다.
Docker CLI 자체 오류는 stderr에 task name만 남기고 exit 1로 반환한다.
`--report-path`를 반복 인자로 받으면 전달된 정확한 디렉터리만 순회해
`**/build/test-results/**/*.xml`과 `**/build/reports/tests/test/**/*.{html,css,js}`에
같은 redaction을 적용한 파일을 `--sanitized-report-dir` 아래 상대 경로로 복사한다.
`report-path`가 존재하지 않거나 report 파일이 없으면 해당 단계는 실패시키고, 원본
report를 sanitized 디렉터리에 절대 링크하지 않는다. collector는 report path를
재귀적으로 무제한 탐색하지 않고 정렬된 path 목록만 방문하며, 전체 sanitized report는
`--max-report-files`(기본 200)와 `--max-report-total-bytes`(기본 2,000,000 bytes)를
동시에 적용한다. 상한 초과 시 추가 파일을 저장하지 않고 manifest에
`report_truncated=true`를 기록하며 exit 1로 종료한다.

`sanitize`는 다음 순서의 stdlib 정규식만 사용한다: (1) `authorization`,
`token`, `password`, `secret`, `api[_-]?key` 등의 key/value를 치환하고,
(2) `scheme://host/path` 형태의 URI를 치환하며, (3) 대문자 환경 변수 assignment와
`payload|message|body|value` 라인 전체를 치환하고, (4) `*Exception`/`*Error`
뒤의 message를 치환한다. 원문 로그는 저장하지 않는다. `--workflow-file`의
`uses: owner/action@ref`를 파싱해 `workflow_action_refs`에 기록하되 ref가 40자리
immutable commit SHA가 아니면 실패시킨다. `docker inspect`의 `RepoDigests`에서
allowlist와 정확히 일치하는 값을 `image_digest`에 기록한다. image ID는 보조 진단
필드로만 기록할 수 있고, `RepoDigests`가 없으면 provenance를 완성할 수 없으므로
실패시킨다.
허용 image allowlist는 `confluentinc/cp-kafka@sha256:a5040785528b0bce3b146febe9fcacdcf2b9b5acb450307f75170ef0e60ec130`과
`redis@sha256:4e070415a5713188624f93815e62d6c6a1fcbb416d2e0b578ab3db627db3a93a`로
고정한다. 해당 image는 `RepoDigests`가 allowlist와 정확히 일치해야 하며, local
image ID만 있거나 예상하지 않은 image면 collector를 실패시킨다.
같은 `sanitize` 규칙을 지정된 `--report-path` 아래의 JUnit XML/HTML에도 적용해
`examples/build/sanitized-test-reports/`에 복사하고, workflow는 원본 report가 아닌
이 경로만 artifact로 업로드한다. 각 파일은 2MB를 넘기지 않으며 전체 합계·파일 수
상한도 적용하고, 원본 경로는 artifact 대상에서 제외한다. 테스트 source의 logging은
module·recordCount·failureKind
같은 구조화 필드만 남기고 payload/message/result/URI를 기록하지 않는다.

- [ ] **Step 2: compile과 test 배열을 분리한다**

workflow의 compile step은 기존 compile task만 `--parallel`로 실행한다. test step은 다음 순서를 정확히 유지한다.

```bash
compile_tasks=(
  :bluetape4k-examples-coroutines-demo:compileKotlin
  :bluetape4k-examples-jpa-blazepersistence-demo:compileKotlin
  :bluetape4k-examples-jpa-querydsl-demo:compileKotlin
  :bluetape4k-examples-redisson-demo:compileKotlin
  :bluetape4k-examples-virtualthreads-demo:compileKotlin
)
test_tasks=(
  :bluetape4k-examples-coroutines-demo:test
  :bluetape4k-examples-jpa-blazepersistence-demo:test
  :bluetape4k-examples-jpa-querydsl-demo:test
  :bluetape4k-examples-redisson-demo:test
  :bluetape4k-examples-virtualthreads-demo:test
)

if [[ -f examples/ktor/idgenerator-ktor-demo/build.gradle.kts ]]; then
  compile_tasks+=( :idgenerator-ktor-demo:compileKotlin )
  test_tasks+=( :idgenerator-ktor-demo:test )
fi
if [[ -f examples/ktor/observability-ktor-demo/build.gradle.kts ]]; then
  compile_tasks+=( :observability-ktor-demo:compileKotlin )
  test_tasks+=( :observability-ktor-demo:test )
fi
if [[ -f examples/spring-boot/idgenerator-spring-boot-demo/build.gradle.kts ]]; then
  compile_tasks+=( :idgenerator-spring-boot-demo:compileKotlin )
  test_tasks+=( :idgenerator-spring-boot-demo:test )
fi
if [[ -f examples/spring-boot/observability-spring-boot-demo/build.gradle.kts ]]; then
  compile_tasks+=( :observability-spring-boot-demo:compileKotlin )
  test_tasks+=( :observability-spring-boot-demo:test )
fi

report_paths=(
  --report-path examples/coroutines-demo/build/test-results/test
  --report-path examples/coroutines-demo/build/reports/tests/test
  --report-path examples/jpa-blazepersistence-demo/build/test-results/test
  --report-path examples/jpa-blazepersistence-demo/build/reports/tests/test
  --report-path examples/jpa-querydsl-demo/build/test-results/test
  --report-path examples/jpa-querydsl-demo/build/reports/tests/test
  --report-path examples/redisson-demo/build/test-results/test
  --report-path examples/redisson-demo/build/reports/tests/test
  --report-path examples/virtualthreads-demo/build/test-results/test
  --report-path examples/virtualthreads-demo/build/reports/tests/test
)
if [[ -f examples/ktor/idgenerator-ktor-demo/build.gradle.kts ]]; then
  report_paths+=(
    --report-path examples/ktor/idgenerator-ktor-demo/build/test-results/test
    --report-path examples/ktor/idgenerator-ktor-demo/build/reports/tests/test
  )
fi
if [[ -f examples/ktor/observability-ktor-demo/build.gradle.kts ]]; then
  report_paths+=(
    --report-path examples/ktor/observability-ktor-demo/build/test-results/test
    --report-path examples/ktor/observability-ktor-demo/build/reports/tests/test
  )
fi
if [[ -f examples/spring-boot/idgenerator-spring-boot-demo/build.gradle.kts ]]; then
  report_paths+=(
    --report-path examples/spring-boot/idgenerator-spring-boot-demo/build/test-results/test
    --report-path examples/spring-boot/idgenerator-spring-boot-demo/build/reports/tests/test
  )
fi
if [[ -f examples/spring-boot/observability-spring-boot-demo/build.gradle.kts ]]; then
  report_paths+=(
    --report-path examples/spring-boot/observability-spring-boot-demo/build/test-results/test
    --report-path examples/spring-boot/observability-spring-boot-demo/build/reports/tests/test
  )
fi

./gradlew "${compile_tasks[@]}" --parallel

set +e
status=0
phase_started=$(date +%s)
for task in "${test_tasks[@]}"; do
  before_ids_file=$(mktemp)
  after_ids_file=$(mktemp)
  docker ps -aq --filter label=org.testcontainers=true | sort -u >"$before_ids_file" || status=1
  ./gradlew "$task" --max-workers=1 || status=1
  docker ps -aq --filter label=org.testcontainers=true | sort -u >"$after_ids_file" || status=1
  new_ids=$(comm -13 "$before_ids_file" "$after_ids_file")
  container_args=()
  while IFS= read -r container_id; do
    [[ -n "$container_id" ]] && container_args+=(--container-id "$container_id")
  done <<<"$new_ids"
  python3 .github/scripts/collect-testcontainers-diagnostics.py \
    --task-name "$task" \
    --output-dir "examples/build/testcontainers-diagnostics/${task//:/_}" \
    --workflow-file .github/workflows/examples.yml \
    --max-total-bytes 2000000 \
    "${container_args[@]}" || status=1
  test -f "examples/build/testcontainers-diagnostics/${task//:/_}/manifest.json" || status=1
  rm -f "$before_ids_file" "$after_ids_file"
done
python3 .github/scripts/collect-testcontainers-diagnostics.py \
  --task-name examples-test-phase-reports \
  --output-dir examples/build/testcontainers-diagnostics/report-scan \
  --workflow-file .github/workflows/examples.yml \
  --sanitized-report-dir examples/build/sanitized-test-reports \
  --max-report-files 200 \
  --max-report-total-bytes 2000000 \
  "${report_paths[@]}" || status=1
test -d examples/build/sanitized-test-reports || status=1
phase_elapsed=$(( $(date +%s) - phase_started ))
printf 'serial_test_phase_seconds=%s\n' "$phase_elapsed" > examples/build/testcontainers-diagnostics/test-phase-timing.txt
(( phase_elapsed <= 2700 )) || status=1
set -e
exit "$status"
```

실제 step에서는 위 배열을 만든 뒤 `./gradlew "${compile_tasks[@]}" --parallel`을
실행한다. 각 test task 직전에
`docker ps -aq --filter label=org.testcontainers=true | sort`를 실행 전 기준 목록으로
저장하고, task 종료 직후 실행 후 기준 목록과 `comm -13`으로 새 ID만 계산해 collector의
반복 `--container-id` 인자로 전달한다. 기준 목록 명령이 Docker 오류를 반환하면
aggregate status를 1로 유지하되 collector는 빈 manifest를 남겨 `if: always()`에서
진단 artifact를 확인할 수 있게 한다. 모든 test task가 끝난 뒤 collector를 정확히
한 번 더 호출해 `report_paths`에 열거한 report 디렉터리만 sanitization하고, 이 호출은
`--max-report-files=200`과 `--max-report-total-bytes=2000000`을 적용한다.
Ktor/Spring Boot 조건부 task는 현재
`build.gradle.kts`가 존재하는 경우에만 compile/test 배열 각각에 추가한다. compile
task와 test task를 같은 `--parallel` 배열에 넣지 않는다. ID 목록은 정렬·중복 제거해
같은 container의 log/manifest를 여러 번 수집하지 않는다.

- [ ] **Step 3: always artifact와 provenance completeness를 고정한다**

`if: always()` upload step은 다음 glob을 포함한다.

```yaml
path: |
  examples/build/sanitized-test-reports/**
  examples/build/testcontainers-diagnostics/**
if-no-files-found: error
```

workflow action은 live tag를 직접 사용하지 않고 2026-08-26에 확인한 immutable
commit SHA로 고정한다. `.github/workflows/examples.yml`의 action 줄은 다음 ref와
`# vN` 주석을 사용하고, checkout에는 `persist-credentials: false`를 설정한다.

```yaml
- uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7
  with:
    persist-credentials: false
- uses: actions/setup-java@b6effb05e454b25005698d916606bdc6ffcbf961 # v5.7.0
- uses: gradle/actions/setup-gradle@9c971963bec38e04b3d30dcc455b5382be2fdbfb # v6.3.0
- uses: actions/upload-artifact@043fb46d1a93c77aae656e7c1c64a875d1fc6a0a # v7
```

각 test invocation 뒤 manifest가 존재하는지 shell에서 검사하고, collector 또는
manifest 누락이면 aggregate status를 1로 만든다. manifest에는 allowlist와 정확히
일치하는 resolved image digest와 immutable workflow action commit ref를 기록한다.
mutable image/action ref는 허용하지 않으며, 이 Epic에서는 공용 image/tag를 변경하지
않는다. 진단 파일은 task별로 새 container ID를 deduplicate하고 총 2,000,000 bytes
상한을 적용한다. `actionlint`, path filter, artifact path와
순서를 정적 검사한다. 기존 workflow timeout 60분에서 15분 headroom을 확보하기
위해 전체 직렬 test phase의 elapsed를 `date +%s`로 측정해
`examples/build/testcontainers-diagnostics/test-phase-timing.txt`에 기록하고, 45분을 초과하면
aggregate status를 1로 만든다. 6-R evidence에는 baseline 병렬 elapsed와 계획된
직렬 elapsed를 함께 남긴다.

- [ ] **Step 4: workflow 정적 검증과 실패 누적을 확인한다**

Run:

```bash
actionlint .github/workflows/examples.yml
python3 -m py_compile .github/scripts/collect-testcontainers-diagnostics.py
python3 -m unittest discover -s .github/scripts -p 'test_*.py'
git diff --check
```

fixture 테스트는 URI userinfo/query, 중첩 JSON/XML payload, 소문자 환경 변수,
multiline `Exception`/`Error` stack trace의 비밀값이 sanitized output에 남지 않는지,
allowlist 밖 image와 local image ID를 거부하는지, report 파일 수·전체 바이트 상한과
`report_truncated=true`를 검증한다. Expected: actionlint, Python syntax, redaction
fixture가 PASS한다. 실제 CI에서는 중간 test failure가 있어도 뒤의 task와 artifact
수집이 실행되고 마지막에 aggregate non-zero가 된다.
fixture는 하이픈이 있는 collector 파일을 `importlib.util.spec_from_file_location`으로
불러오며 실제 Docker 호출은 mock subprocess로 대체해 secret 원문을 남기지 않는다.

## Task 6: coroutines README locale parity를 맞춘다

**Files:**

- Modify: `examples/coroutines-demo/README.md`
- Modify: `examples/coroutines-demo/README.ko.md`

- [ ] **Step 1: example table과 실행 명령을 같은 순서로 갱신한다**

두 README의 `CallbackFlowExamples.kt` 행에 실제 Kafka producer callback, bounded backpressure, cancellation, cleanup 설명을 추가한다. 실행 섹션에는 다음 명령을 그대로 넣는다.

```bash
./gradlew :bluetape4k-examples-coroutines-demo:test \
  --tests 'io.bluetape4k.examples.coroutines.flow.CallbackFlowExamples' \
  --no-configuration-cache --max-workers=1
```

Docker daemon/Testcontainers dynamic port가 필요하고, topic은 매 test마다 unique하며
poll·producer close는 bounded timeout이라는 설명을 두 locale에 같은 의미로 넣는다.
metadata 순서는 보장하지 않고 adapter는 retry하지 않으며, failure/cancellation 시
부분 결과가 발생할 수 있다는 caller 계약도 두 locale에 기록한다.

- [ ] **Step 2: README locale parity를 검사한다**

Run:

```bash
git diff --check
for file in examples/coroutines-demo/README.md examples/coroutines-demo/README.ko.md; do
  rg -F -- "bluetape4k-examples-coroutines-demo:test" "$file"
  rg -F -- "CallbackFlowExamples" "$file"
  rg -F -- "--no-configuration-cache" "$file"
  rg -F -- "--max-workers=1" "$file"
  rg -F -- "Testcontainers" "$file"
  rg -F -- "Docker" "$file"
  rg -F -- "timeout" "$file"
done
python3 - <<'PY'
from pathlib import Path

english = Path("examples/coroutines-demo/README.md").read_text()
korean = Path("examples/coroutines-demo/README.ko.md").read_text()
required = (
    ":bluetape4k-examples-coroutines-demo:test",
    "CallbackFlowExamples",
    "--no-configuration-cache",
    "--max-workers=1",
    "Testcontainers",
    "Docker",
    "timeout",
)
assert all(token in english and token in korean for token in required)
PY
```

Expected: 두 파일의 task name과 example name이 일치하고 한국어 문서에는 독자용 설명이 자연스럽게 남는다.

## Task 7: child-level verification과 6-R module review evidence를 만든다

**Files:**

- Create: `docs/superpowers/reviews/2026-08-26-epic-1422-1347-module-6r.md`
- Modify: `examples/coroutines-demo/build.gradle.kts` only when verification finds a dependency defect
- Modify: `examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/flow/CallbackFlowExamples.kt` only when verification finds a behavior defect
- Modify: `examples/coroutines-demo/README.md` only when verification finds an English parity defect
- Modify: `examples/coroutines-demo/README.ko.md` only when verification finds a Korean parity defect
- Modify: `.github/scripts/collect-testcontainers-diagnostics.py` only when verification finds a diagnostics defect
- Modify: `.github/workflows/examples.yml` only when verification finds a CI ordering or artifact defect

- [ ] **Step 1: targeted와 full module test를 순차 실행한다**

```bash
./gradlew :bluetape4k-examples-coroutines-demo:test \
  --tests 'io.bluetape4k.examples.coroutines.flow.CallbackFlowExamples' \
  --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-examples-coroutines-demo:test \
  --no-configuration-cache --max-workers=1
./gradlew :bluetape4k-examples-coroutines-demo:detekt \
  --no-configuration-cache --max-workers=1
git diff --check
```

Expected: 세 명령이 모두 PASS하고, Testcontainers 종료 후 다음 child로 넘어갈 수 있도록 남은 container와 dynamic topic을 확인한다.

- [ ] **Step 2: 6-R evidence를 기록한다**

검토 문서에는 touched file, 실제 command/output, bluetape assertions 사용 위치, `runSuspendIO`/IO dispatcher, callback first-cause/cleanup, Docker precondition, known gaps를 기록한다. six perspective 결과가 P0=0/P1=0이 될 때까지 수정하고 `audit-korean-terms.mjs`를 실행한다.

- [ ] **Step 3: parent child를 merge-ready 상태로 고정한다**

PR 생성 전 child issue #1347의 `## DoD Status`와 parent PR body에 exact head, required check count, 6-R artifact, known gaps를 기록한다. child PR body에는 `Closes #1347`을 사용해 child merge 시 이슈가 자동 종료되도록 하고, 최종 child #1353 PR에만 `Closes #1422`를 둔다. #1353 branch는 이 parent head를 base로 만들며, parent merge 전 temporary base 절차는 승인된 설계 명세를 그대로 따른다. 최종 merge 후 live GitHub에서 두 child issue와 Epic #1422의 closed/DoD 상태를 재확인한다.

## Kafka plan traceability

| 명세 acceptance | 계획 task | 증거 |
|---|---|---|
| 실제 broker callbackFlow | 1, 3, 4 | targeted Kafka test와 consumer poll |
| success/failure/cancellation/backpressure/shutdown | 2, 3, 4 | deterministic fixture counters와 failure matrix |
| bluetape assertions | 2, 4, 7 | `assertFailsWith`, `shouldBeEqualTo`, collection matchers |
| README parity | 6 | 두 locale diff/readback |
| Testcontainers 순차 실행 | 5, 7 | actionlint와 CI task output |
| diagnostics/provenance follow-up | 5 | manifest 존재·redaction 검사 |
| exact-head/stacked delivery | 7 | child SHA, parent merge commit, base retarget evidence |

## Rollback과 stop condition

Kafka adapter/test만 실패하면 Task 3–4 commit을 되돌리고 기존 example test를 복구한다. workflow 변경이 60분 budget을 넘기거나 diagnostics completeness가 실패하면 workflow commit만 revert하고 child code evidence를 유지한다. exact-head CI failure, unresolved P1, producer/consumer cleanup timeout이면 merge-ready를 중단하고 해당 task와 `## DoD Status`를 다시 검증한다. production API, dependency version, image/tag 변경이 발견되면 이 계획 범위를 벗어나므로 별도 승인 전에는 진행하지 않는다.

unique topic은 Testcontainers broker 수명과 함께 폐기되므로 정상 경로에서 별도
삭제 명령을 추가하지 않는다. 장시간 shared broker에서 잔여 topic을 수집해야 하는
요구가 생기면 별도 운영 이슈로 다루며 이 child의 cleanup 계약을 확장하지 않는다.

## 계획 완료 조건

- 모든 checkbox가 실제 commit과 fresh command evidence로 완료된다.
- Kafka child 6-R의 최신 P0/P1이 0이다.
- `README.md`와 `README.ko.md`가 명령·lifecycle·timeout 계약에서 일치한다.
- code, workflow, docs, test 결과가 parent PR의 마지막 `## DoD Status`에 연결된다.
