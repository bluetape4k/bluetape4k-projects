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
- Modify: `.github/workflows/examples.yml` — compile 병렬 phase와 Testcontainers test 순차 phase를 분리하고 aggregate failure/artifact를 고정한다.

이 계획은 production Kafka API, 새 library module, catalog version, Kafka image/tag를 변경하지 않는다. #1353 소유 파일은 이 계획에서 수정하지 않는다.

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
    producer.closeCount shouldBeEqualTo 1
    producer.callbackCount shouldBeEqualTo 1
}

@Test
fun `backpressure fails without dropping callback and closes producer once`() = runSuspendIO {
    val producer = TrackingProducer()
    val collectorGate = CompletableDeferred<Unit>()
    val collection = async {
        var received = 0
        producerResults(
            records = flowOf(record("one"), record("two"), record("three")),
            producerFactory = { producer.producer },
            channelCapacity = 1,
            maxInFlight = 3,
        ).collect {
            val index = received
            received += 1
            if (index == 0) collectorGate.await()
        }
    }

    producer.thirdCallback.await()
    collectorGate.complete(Unit)
    val error = assertFailsWith<IllegalStateException> {
        collection.await()
    }

    error.message shouldBeEqualTo "callback buffer is full"
    producer.closeCount shouldBeEqualTo 1
    producer.callbackCount shouldBeEqualTo 3
}

@Test
fun `in-flight permit is released after callback`() = runSuspendIO {
    val producer = TrackingProducer()

    producerResults(
        records = flowOf(record("permit-one"), record("permit-two")),
        producerFactory = { producer.producer },
        channelCapacity = 1,
        maxInFlight = 1,
    ).toList()

    producer.callbackCount shouldBeEqualTo 2
}

@Test
fun `collector cancellation rethrows CancellationException and closes producer once`() = runSuspendIO {
    val producer = TrackingProducer(holdCallbacks = true)
    val task = async {
        producerResults(flowOf(record("cancel")), { producer.producer }).toList()
    }

    producer.sendStarted.await()
    task.cancel()
    assertFailsWith<CancellationException> { task.await() }

    producer.closeCount shouldBeEqualTo 1
    producer.pendingSend.isCancelled shouldBeEqualTo true
    producer.fireLateCallback()
    producer.lateCallbackCount shouldBeEqualTo 1
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

    producer.flushCount shouldBeEqualTo 1
    producer.closeCount shouldBeEqualTo 1
    producer.callbackCount shouldBeEqualTo 1
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
    producer.closeCount shouldBeEqualTo 1
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
    producer.closeCount shouldBeEqualTo 1
    producer.pendingSend.isCancelled shouldBeEqualTo true
}
```

`record(value)`는 다음 private helper로 unique topic을 만들고 topic/key/value/header 길이 계약을 검사한다.

```kotlin
private fun record(value: String): ProducerRecord<String, String> {
    val topic = "epic-1422-${Base58.randomString(8)}"
    require(topic.length <= 128)
    require(value.toByteArray(Charsets.UTF_8).size <= 1024)
    return ProducerRecord(topic, "key", value)
}
```

`TrackingProducer`는 MockK로 감싼 test-only `Producer<String, String>` probe다. 다음
설정을 생성자에서 받고, 테스트 factory가 `producer`를 반환하도록 고정한다.

```kotlin
private class TrackingProducer(
    private val callbackError: Exception? = null,
    private val sendError: Exception? = null,
    val closeError: Exception? = null,
    private val holdCallbacks: Boolean = false,
) {
    val producer: Producer<String, String> = mockk(relaxed = true)
    val callbackCount = AtomicInteger()
    val closeCount = AtomicInteger()
    val flushCount = AtomicInteger()
    val lateCallbackCount = AtomicInteger()
    val sendStarted = CompletableDeferred<Unit>()
    val thirdCallback = CompletableDeferred<Unit>()
    val pendingCallbacks = ConcurrentLinkedQueue<Callback>()
    val pendingSend = CompletableFuture<RecordMetadata>()
    private val closed = AtomicBoolean()
    private val metadata = mockk<RecordMetadata>(relaxed = true)

    init {
        every { producer.send(any<ProducerRecord<String, String>>(), any()) } answers {
            if (sendError != null) throw sendError
            val callback = secondArg<Callback>()
            if (holdCallbacks) {
                pendingCallbacks += callback
            } else {
                callbackCount.incrementAndGet()
                callback.onCompletion(metadata, callbackError)
                if (callbackCount.get() == 3) thirdCallback.complete(Unit)
            }
            sendStarted.complete(Unit)
            if (holdCallbacks) pendingSend else CompletableFuture.completedFuture(metadata)
        }
        every { producer.flush() } answers { flushCount.incrementAndGet() }
        every { producer.close(any<Duration>()) } answers {
            closeCount.incrementAndGet()
            closed.set(true)
            closeError?.let { throw it }
        }
    }

    fun fireLateCallback() {
        pendingCallbacks.poll()?.let { callback ->
            if (closed.get()) lateCallbackCount.incrementAndGet()
            callback.onCompletion(metadata, null)
        }
    }
}
```

기본 성공 fixture는 callback을 즉시 호출하고, held fixture는 `pendingCallbacks`를
보관해 drain timeout과 cancellation을 결정적으로 만든다. `maxInFlight=1`에서
두 record가 모두 완료되는 테스트가 callback 공통 `finally`의 permit release를
간접적으로 증명한다. close 이후 callback은 `lateCallbackCount`만 증가시킨다.
`Producer`의 다른 메서드는 MockK relaxed
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

Step 2–3에서 이 signature의 함수 body를 완성한다. Flow collection마다
`producerFactory()`를 한 번 호출하고, `Semaphore(maxInFlight)`와
`AtomicReference<Throwable?>`를 collection-local 상태로 만든다. callback 성공은
`channel.trySend(metadata)`로 전달하고, callback 성공·실패·동기 `send` 예외 모두에서
공통 `finally`로 permit/in-flight를 정확히 한 번만 해제한다.
각 `send`의 반환 Future를 in-flight 항목과 함께 보관해 cancellation 또는 drain
deadline에서 `cancel(false)`하고, deterministic probe의 `pendingSend`로 그 결과를
검증한다.

- [ ] **Step 2: callback failure와 full-buffer terminal path를 구현한다**

첫 terminal cause는 다음 규칙을 사용한다.

```kotlin
if (terminalCause.compareAndSet(null, cause)) {
    worker.cancel(CancellationException("producer terminal failure", cause))
    upstream.cancel(CancellationException("producer terminal failure", cause))
    channel.close(cause)
} else {
    log.debug { "late Kafka callback ignored; failureKind=$failureKind" }
}
```

`trySend`가 full이면 `IllegalStateException("callback buffer is full")`을 위 CAS에 넣고 worker/upstream을 취소한다. 이미 닫힌 channel은 downstream cancellation이면 새 cause로 덮지 않는다. callbackFlow channel과 producer close는 각각 한 번만 실행하며, cleanup 예외는 first cause의 `suppressed`에 붙인다.

- [ ] **Step 3: awaitClose와 bounded cleanup을 구현한다**

`awaitClose`에서는 cancellation signal만 전달하고 blocking Kafka API를 호출하지 않는다. worker의 `finally` 안에서 다음 순서를 지킨다.

1. `withContext(NonCancellable + Dispatchers.IO)`로 진입한다.
2. callback drain과 `flush()`를 30초 `withTimeout`으로 기다린다.
3. deadline을 넘기면 `TimeoutCancellationException`을 first cause로 CAS하고 pending send를 취소한다.
4. `producer.close(Duration.ofSeconds(5))`를 한 번 실행하고 예외를 suppressed로 연결한다.
5. `CancellationException`을 broad catch에서 삼키지 않고 재전파한다.

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

- [ ] **Step 1: Testcontainers precondition과 실제 fixture를 사용한다**

`runSuspendIO(timeout = 120.seconds)`에서 `KafkaServer.Launcher.kafka`를 참조하고, `createStringProducer()`와 `createStringConsumer()`를 각각 한 번 생성한다. topic은 `"epic-1422-callback-" + Base58.randomString(8)`로 만들고, record 수는 128개 이하, value는 1 KiB 이하로 제한한다. producer와 consumer가 broker 자체를 종료하지 않고 collection-scoped/test-owned resource만 닫도록 `use`와 bounded close를 적용한다.

- [ ] **Step 2: success cardinality와 eventual broker result를 검증한다**

```kotlin
@Test
fun `real Kafka producer callbacks become metadata flow`() = runSuspendIO(timeout = 120.seconds) {
    val topic = "epic-1422-callback-${Base58.randomString(8)}"
    val records = (0 until 8).map { index ->
        ProducerRecord(topic, "key-$index", "value-$index")
    }

    val consumer = KafkaServer.Launcher.createStringConsumer()
    try {
        consumer.subscribe(listOf(topic))
        val metadata = producerResults(
            records = records.asFlow(),
            producerFactory = { KafkaServer.Launcher.createStringProducer() },
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
  --workflow-file .github/workflows/examples.yml
```

스크립트는 `docker ps -a --filter label=org.testcontainers=true`로 container id를 읽고, 각 container에 대해 `docker inspect`의 image/name/created와 `docker logs --tail 200`만 수집한다. 출력은 task 이름으로 정규화한 JSON manifest와 `*.log`로 저장하며, token·URI·환경 변수·payload·exception message는 `[REDACTED]`로 치환한다. container log가 없으면 빈 manifest를 만들고 exit 0으로 종료한다. Docker CLI 자체 오류는 stderr에 task name만 남기고 exit 1로 반환한다.

`sanitize`는 다음 순서의 stdlib 정규식만 사용한다: (1) `authorization`,
`token`, `password`, `secret`, `api[_-]?key` 등의 key/value를 치환하고,
(2) `scheme://host/path` 형태의 URI를 치환하며, (3) 대문자 환경 변수 assignment와
`payload|message|body|value` 라인 전체를 치환하고, (4) `*Exception`/`*Error`
뒤의 message를 치환한다. 원문 로그는 저장하지 않는다. `--workflow-file`의
`uses: owner/action@ref`를 파싱해 `workflow_action_refs`에 기록하고,
`docker inspect`의 `RepoDigests` 또는 image ID를 `image_digest`에 기록한다.

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

./gradlew "${compile_tasks[@]}" --parallel

set +e
status=0
for task in "${test_tasks[@]}"; do
  ./gradlew "$task" --max-workers=1 || status=1
  python3 .github/scripts/collect-testcontainers-diagnostics.py \
    --task-name "$task" \
    --output-dir "examples/build/testcontainers-diagnostics/${task//:/_}" \
    --workflow-file .github/workflows/examples.yml || status=1
  test -f "examples/build/testcontainers-diagnostics/${task//:/_}/manifest.json" || status=1
done
set -e
exit "$status"
```

실제 step에서는 위 배열을 만든 뒤 `./gradlew "${compile_tasks[@]}" --parallel`을
실행한다. Ktor/Spring Boot 조건부 task는 현재 `build.gradle.kts`가 존재하는
경우에만 compile/test 배열 각각에 추가한다. compile task와 test task를 같은
`--parallel` 배열에 넣지 않는다.

- [ ] **Step 3: always artifact와 provenance completeness를 고정한다**

`if: always()` upload step은 다음 glob을 포함한다.

```yaml
path: |
  examples/**/build/test-results/**
  examples/**/build/reports/tests/test/**
  examples/build/testcontainers-diagnostics/**
if-no-files-found: error
```

각 test invocation 뒤 manifest가 존재하는지 shell에서 검사하고, collector 또는
manifest 누락이면 aggregate status를 1로 만든다. manifest에는 resolved image
digest와 workflow action ref를 기록하되, 이 Epic에서는 mutable image/action을
변경하거나 pinning하지 않는다. `actionlint`, path filter, artifact path와
순서를 정적 검사한다.

- [ ] **Step 4: workflow 정적 검증과 실패 누적을 확인한다**

Run:

```bash
actionlint .github/workflows/examples.yml
python3 -m py_compile .github/scripts/collect-testcontainers-diagnostics.py
git diff --check
```

Expected: actionlint와 Python syntax가 PASS한다. 실제 CI에서는 중간 test failure가 있어도 뒤의 task와 artifact 수집이 실행되고 마지막에 aggregate non-zero가 된다.

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
rg -n "bluetape4k-examples-coroutines-demo:test|CallbackFlowExamples|Testcontainers|Docker|timeout" \
  examples/coroutines-demo/README.md examples/coroutines-demo/README.ko.md
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

## 계획 완료 조건

- 모든 checkbox가 실제 commit과 fresh command evidence로 완료된다.
- Kafka child 6-R의 최신 P0/P1이 0이다.
- `README.md`와 `README.ko.md`가 명령·lifecycle·timeout 계약에서 일치한다.
- code, workflow, docs, test 결과가 parent PR의 마지막 `## DoD Status`에 연결된다.
