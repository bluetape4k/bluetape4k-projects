# Issue #1051 Context Propagation Conformance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** coroutine, Reactor, task executor, Spring Observation, Ktor request 경계가 동일한 provider-neutral assertion으로 parent visibility, terminal 분류, cleanup, isolation을 결정적으로 증명하도록 한다.

**Architecture:** `:bluetape4k-junit5`는 framework type을 모르는 immutable snapshot과 assertion만 제공한다. `:bluetape4k-opentelemetry`, `:bluetape4k-spring-boot-core`, `:bluetape4k-ktor-observability`의 test source가 실제 bridge를 실행하고 관측값을 snapshot으로 변환한다. production code, build script, 전역 OTel/Reactor 상태는 변경하지 않는다.

**Tech Stack:** Kotlin 2.3, Java 21, JUnit 5, kotlinx.coroutines, OpenTelemetry Java API/SDK, Reactor, Micrometer Observation, Spring Boot 4, Ktor `testApplication`, Gradle

---

## 0. 실행 계약

- 설계 기준: `docs/superpowers/specs/2026-07-28-issue-1051-context-propagation-conformance-design.md`
- 적용 패턴:
  - `bluetape-kotlin-patterns`: public KDoc, immutable value, 명시적 serial UID, 파일 크기/책임 제한
  - `kotlin-coroutines-skill`: cancellation 재전파, `withTimeout` semantic deadline, `finally` cleanup, 구조화된 child lifecycle
  - `ecc-springboot-kotlin`: Spring test slice와 registry lifecycle, production surface 비변경
- 작업 순서: Task 1부터 Task 8까지 순차 실행한다. 같은 task 안에서는 RED → 최소 GREEN → refactor → targeted test 순서를 지킨다.
- 금지:
  - 새 dependency, module, production API, global OTel setter, Reactor global hook
  - `Thread.sleep` 또는 ordering용 `delay`
  - raw marker를 assertion message 또는 log에 포함
  - Testcontainers, production exporter/collector, 외부 HTTP server
- 중단 조건: production 수정이나 새 dependency가 필요하면 구현을 중단하고 spec review를 다시 연다.
- 테스트 전체 guard:

```kotlin
@Timeout(
    value = 15,
    unit = TimeUnit.SECONDS,
    threadMode = Timeout.ThreadMode.SAME_THREAD,
)
```

- 공통 시간 경계:

```kotlin
private val semanticDeadline = 250.milliseconds
private val hangGuard = DEFAULT_CANCELLATION_CONTRACT_TIMEOUT

init {
    check(semanticDeadline < hangGuard)
}
```

- 결정성 증명: 모든 scenario는 기본 full suite에서 1회 실행한다. 확률적 반복 대신 test-private event ledger가 두 participant의 `READY`가 모두 기록된 뒤에만 `RELEASE`되었는지, cleanup probe가 terminal/finally 뒤에 실행됐는지 exact partial order로 assertion한다. 별도 반복/stress task는 이 issue와 최종 evidence에 추가하지 않는다.

## Acceptance criteria 매핑

| Issue 기준 | 구현 task | 증명 |
|---|---|---|
| coroutine/Reactor/executor parent propagation | 3, 4, 5 | 각 경계 success/failure/cancellation/deadline/isolation 5개 |
| cancellation/deadline cleanup | 3, 4, 5, 6, 7 | 실제 terminal과 caller/worker/request probe |
| request leakage 없음 | 3, 4, 5, 6, 7 | failure-aware A/B barrier와 후속 unwrapped/probe 실행 |
| deterministic tests | 2–7 | 명시적 gate, 5초 hang guard, 15초 aggregate guard |
| Spring/Ktor 동일 assertion | 6, 7 | shared fixture 직접 호출 |
| 외부 telemetry/backend 불필요 | 3–7 | local SDK/registry, in-process application |
| interception은 소비 module 유지 | 1, 3–7 | shared public API에 framework type 없음 |
| 실패 메시지에 민감값 없음 | 1 | CR/LF canary negative self-test |
| Spec §5.2 public KDoc/example | 1 | 모든 enum/snapshot/assertion declaration별 English KDoc example ledger |
| Spec §5.2 safe diagnostics | 1 | boundary/scenario/alias/field/mode enum 좌표와 `values redacted` exact-message tests |
| Spec §9 compatibility | 1, 8 | enum additive/non-exhaustive 안내와 constructor compatibility 경고 |
| Spec §9 bilingual README | 8 | complete propagation/isolation examples와 parity validation script |

## Task 1: provider-neutral fixture를 TDD로 추가

**Files:**

- Create: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/observability/ContextPropagationConformance.kt`
- Create: `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/observability/ContextPropagationConformanceTest.kt`
- Reference: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/observability/HttpOperationObservabilityConformance.kt`
- Reference: `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/observability/HttpOperationObservabilityConformanceTest.kt`

**Complexity:** L

**Dependencies:** 없음

**Write scope:** 위 두 새 파일만

### Step 1.1 — RED: public contract의 정상/실패 의미를 먼저 고정

- [ ] 최소 10개 self-test를 만든다.
- [ ] observation point exact set, terminal 구분, cleanup location exact set, root marker 금지, alias uniqueness, `EXACT`/`ABSENT`/`NOT_IN`, minimum observation count, redaction을 포함한다.
- [ ] failure message canary는 `secret-parent\r\nforged-log`를 사용한다. observation-point set, propagation marker, cleanup, isolation `EXACT`, isolation `NOT_IN`, invalid expectation의 각 failure path에서 메시지에 `secret-parent`, `forged-log`, `\r`, `\n`이 없음을 parameterized test로 확인한다.
- [ ] fixture는 성공/실패 어느 경로에서도 marker를 log/print하지 않는다. 별도 logger를 추가하지 않고 self-test appender에 fixture-originated event가 없음을 확인한다.

```kotlin
class ContextPropagationConformanceTest {

    @Test
    fun `accepts matching propagation snapshot`() {
        assertContextPropagationConformance(
            observation = propagationObservation(),
            expectation = propagationExpectation(),
        )
    }

    @Test
    fun `rejects mismatched observation point set without exposing marker`() {
        val canary = "secret-parent\r\nforged-log"
        val error = assertFailsWith<AssertionError> {
            assertContextPropagationConformance(
                propagationObservation(
                    markerObservations = listOf(
                        ContextMarkerObservation(ContextObservationPoint.BOUNDARY_ENTER, canary),
                    ),
                ),
                propagationExpectation(),
            )
        }
        error.message.shouldNotContain("secret-parent")
        error.message.shouldNotContain("forged-log")
        error.message.shouldNotContain("\r")
        error.message.shouldNotContain("\n")
    }

    @Test
    fun `keeps failure cancellation and deadline terminals distinct`() {
        ContextPropagationTerminal.entries
            .filterNot { it == ContextPropagationTerminal.SUCCESS }
            .forEach { actual ->
                assertFailsWith<AssertionError> {
                    assertContextPropagationConformance(
                        propagationObservation(terminal = actual),
                        propagationExpectation(
                            expectedTerminal = ContextPropagationTerminal.SUCCESS,
                        ),
                    )
                }
            }
    }

    @Test
    fun `rejects root string marker`() {
        assertFailsWith<AssertionError> {
            assertContextPropagationConformance(
                propagationObservation(
                    markerObservations = listOf(
                        ContextMarkerObservation(
                            ContextObservationPoint.BOUNDARY_ENTER,
                            "root",
                        ),
                    ),
                ),
                propagationExpectation(),
            )
        }
    }

    @Test
    fun `accepts exact absent and not-in isolation samples`() {
        assertContextIsolation(
            observation = isolationObservation(),
            expectation = isolationExpectation(),
        )
    }

    @Test
    fun `rejects duplicate aliases and exact markers`() {
        assertFailsWith<AssertionError> {
            assertContextIsolation(
                isolationObservation(),
                isolationExpectation(
                    samples = listOf(
                        exactExpectation(ContextRequestAlias.REQUEST_A, "parent-A"),
                        exactExpectation(ContextRequestAlias.REQUEST_A, "parent-A"),
                    ),
                ),
            )
        }
    }

    @Test
    fun `rejects mode-field combinations that are not meaningful`() {
        val invalid = listOf(
            ContextIsolationSampleExpectation(
                ContextRequestAlias.REQUEST_A,
                ContextMarkerExpectationMode.EXACT,
            ),
            ContextIsolationSampleExpectation(
                ContextRequestAlias.REQUEST_A,
                ContextMarkerExpectationMode.ABSENT,
                expectedMarker = "parent-A",
            ),
            ContextIsolationSampleExpectation(
                ContextRequestAlias.PROBE,
                ContextMarkerExpectationMode.NOT_IN,
            ),
        )
        invalid.forEach { sample ->
            assertFailsWith<AssertionError> {
                assertContextIsolation(
                    isolationObservation(),
                    isolationExpectation(samples = listOf(sample)),
                )
            }
        }
    }

    @Test
    fun `rejects insufficient isolation observations`() {
        assertFailsWith<AssertionError> {
            assertContextIsolation(
                isolationObservation(
                    samples = listOf(
                        ContextIsolationSample(ContextRequestAlias.REQUEST_A, listOf("parent-A")),
                    ),
                ),
                isolationExpectation(
                    samples = listOf(
                        exactExpectation(
                            ContextRequestAlias.REQUEST_A,
                            "parent-A",
                            minimumObservationCount = 2,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `rejects cleanup location mismatch`() {
        assertFailsWith<AssertionError> {
            assertContextPropagationConformance(
                propagationObservation(
                    cleanupProbes = listOf(
                        ContextCleanupProbe(ContextProbeLocation.WORKER, null),
                    ),
                ),
                propagationExpectation(),
            )
        }
    }

    @Test
    fun `rejects not-in sample containing forbidden marker`() {
        assertFailsWith<AssertionError> {
            assertContextIsolation(
                isolationObservation(
                    samples = listOf(
                        ContextIsolationSample(ContextRequestAlias.PROBE, listOf("parent-A")),
                    ),
                ),
                isolationExpectation(
                    samples = listOf(
                        ContextIsolationSampleExpectation(
                            ContextRequestAlias.PROBE,
                            ContextMarkerExpectationMode.NOT_IN,
                            forbiddenMarkers = listOf("parent-A", "parent-B"),
                        ),
                    ),
                ),
            )
        }
    }
}
```

- [ ] RED 명령:

```bash
./gradlew :bluetape4k-junit5:test \
  --tests "io.bluetape4k.junit5.observability.ContextPropagationConformanceTest"
```

**Expected RED:** 새 public type과 assertion symbol을 찾지 못해 test compilation이 실패한다.

### Step 1.2 — GREEN: framework-neutral snapshot과 redacted assertion 구현

- [ ] 승인된 spec의 enum/data class/function signature를 그대로 구현한다.
- [ ] 모든 data class를 `Serializable`로 만들고 저장소 선례대로 companion에 `private const val serialVersionUID: Long = 1L`을 둔다.
- [ ] 모든 public declaration에 English KDoc과 해당 declaration을 직접 사용하는 최소 예제를 작성한다. KDoc은 test-owned synthetic marker만 허용하고 production request ID, user data, external trace ID를 금지하며, `Serializable` snapshot을 persistent/wire format으로 저장하지 말아야 한다는 privacy/compatibility contract를 포함한다.
- [ ] public enum KDoc은 future value 추가가 additive임을 명시하고 caller의 exhaustive `when`에 `else`를 요구한다. data-class KDoc은 constructor 변경이 compatibility-sensitive하므로 저장/구조분해/wire contract로 사용하지 말라고 명시한다.
- [ ] validation failure는 raw value 없이 enum과 관계만 표시한다.

**KDoc example ledger:**

| Public declaration | KDoc example proof |
|---|---|
| `ContextPropagationBoundary` | `val boundary = ContextPropagationBoundary.COROUTINE` |
| `ContextPropagationScenario` | `val scenario = ContextPropagationScenario.SUCCESS` |
| `ContextPropagationTerminal` | `val terminal = ContextPropagationTerminal.SUCCESS` |
| `ContextProbeLocation` | `val location = ContextProbeLocation.CALLER` |
| `ContextRequestAlias` | `val alias = ContextRequestAlias.SINGLE` |
| `ContextObservationPoint` | `val point = ContextObservationPoint.BOUNDARY_ENTER` |
| `ContextMarkerExpectationMode` | `val mode = ContextMarkerExpectationMode.EXACT` |
| `ContextMarkerObservation` | construct one synthetic enter observation |
| `ContextMarkerExpectation` | construct the matching enter expectation |
| `ContextCleanupProbe` | construct caller/root probe with `null` |
| `ContextCleanupExpectation` | construct caller/root expectation with `null` |
| `ContextPropagationObservation` | complete success snapshot with enter/after/before points |
| `ContextPropagationExpectation` | complete matching success expectation |
| `ContextIsolationSample` | A sample with two synthetic observations |
| `ContextIsolationSampleExpectation` | matching `EXACT` expectation |
| `ContextIsolationObservation` | A/B/probe isolation snapshot |
| `ContextIsolationExpectation` | A/B `EXACT` plus probe `NOT_IN` |
| `assertContextPropagationConformance` | complete observation/expectation invocation |
| `assertContextIsolation` | complete isolation invocation |

- [ ] uniform safe diagnostic shape를 exact-message test로 고정한다. marker 원문 대신 `relation=mismatch`만 쓰고 safe enum 좌표만 포함한다.

```kotlin
private fun propagationFailure(
    observation: ContextPropagationObservation,
    field: String,
    actual: String = "redacted",
    expected: String = "redacted",
): Nothing =
    fail(
        "Context propagation mismatch: " +
                "boundary=${observation.boundary}, " +
                "scenario=${observation.scenario}, " +
                "alias=${observation.requestAlias}, " +
                "field=$field, actual=$actual, expected=$expected, " +
                "relation=mismatch; values redacted",
    )

private fun isolationFailure(
    observation: ContextIsolationObservation,
    expectation: ContextIsolationSampleExpectation,
    field: String,
): Nothing =
    fail(
        "Context isolation mismatch: " +
                "boundary=${observation.boundary}, " +
                "alias=${expectation.requestAlias}, " +
                "mode=${expectation.mode}, " +
                "field=$field, relation=mismatch; values redacted",
    )
```

```kotlin
private const val ROOT_MARKER = "root"

private fun fail(message: String): Nothing = throw AssertionError(message)

private fun validateMarker(marker: String?, role: String) {
    if (marker == ROOT_MARKER) {
        fail("Context marker $role must use null for root")
    }
}

private fun <T> requireUnique(values: List<T>, role: String) {
    if (values.size != values.toSet().size) {
        fail("Context $role must be unique")
    }
}

fun assertContextPropagationConformance(
    observation: ContextPropagationObservation,
    expectation: ContextPropagationExpectation,
) {
    if (observation.boundary != expectation.boundary) {
        propagationFailure(
            observation,
            "boundary",
            observation.boundary.name,
            expectation.boundary.name,
        )
    }
    if (observation.scenario != expectation.scenario) {
        propagationFailure(
            observation,
            "scenario",
            observation.scenario.name,
            expectation.scenario.name,
        )
    }
    if (observation.requestAlias != expectation.requestAlias) {
        propagationFailure(
            observation,
            "requestAlias",
            observation.requestAlias.name,
            expectation.requestAlias.name,
        )
    }
    if (observation.terminal != expectation.expectedTerminal) {
        propagationFailure(
            observation,
            "terminal",
            observation.terminal.name,
            expectation.expectedTerminal.name,
        )
    }

    val actualPoints = observation.markerObservations.map { it.point }
    val expectedPoints = expectation.markerExpectations.map { it.point }
    requireUnique(actualPoints, "observation points")
    requireUnique(expectedPoints, "expectation points")
    if (actualPoints.toSet() != expectedPoints.toSet()) {
        propagationFailure(observation, "observationPointSet")
    }
    expectation.markerExpectations.forEach { expected ->
        validateMarker(expected.expectedMarker, "expectation")
        val actual = observation.markerObservations.single { it.point == expected.point }
        validateMarker(actual.observedMarker, "observation")
        if (actual.observedMarker != expected.expectedMarker) {
            propagationFailure(observation, "marker.${expected.point}")
        }
    }

    assertCleanup(observation.cleanupProbes, expectation.cleanupExpectations)
}

fun assertContextIsolation(
    observation: ContextIsolationObservation,
    expectation: ContextIsolationExpectation,
) {
    if (observation.boundary != expectation.boundary) {
        fail(
            "Context isolation mismatch: boundary=${observation.boundary}, " +
                    "field=boundary, relation=mismatch; values redacted",
        )
    }
    val actualAliases = observation.samples.map { it.requestAlias }
    val expectedAliases = expectation.samples.map { it.requestAlias }
    requireUnique(actualAliases, "isolation observation aliases")
    requireUnique(expectedAliases, "isolation expectation aliases")
    if (actualAliases.toSet() != expectedAliases.toSet()) {
        fail(
            "Context isolation mismatch: boundary=${observation.boundary}, " +
                    "field=aliasSet, relation=mismatch; values redacted",
        )
    }

    val exactMarkers = expectation.samples
        .filter { it.mode == ContextMarkerExpectationMode.EXACT }
        .mapNotNull { it.expectedMarker }
    requireUnique(exactMarkers, "exact expectation markers")

    expectation.samples.forEach { expected ->
        validateIsolationExpectation(expected)
        val actual = observation.samples.single { it.requestAlias == expected.requestAlias }
        if (actual.observedMarkers.size < expected.minimumObservationCount) {
            isolationFailure(observation, expected, "observationCount")
        }
        actual.observedMarkers.forEach { validateMarker(it, "isolation observation") }
        val matches = when (expected.mode) {
            ContextMarkerExpectationMode.EXACT ->
                actual.observedMarkers.all { it == expected.expectedMarker }
            ContextMarkerExpectationMode.ABSENT ->
                actual.observedMarkers.all { it == null }
            ContextMarkerExpectationMode.NOT_IN ->
                actual.observedMarkers.all {
                    it != null && it !in expected.forbiddenMarkers
                }
        }
        if (!matches) {
            isolationFailure(observation, expected, "markerRelation")
        }
    }

    assertCleanup(observation.cleanupProbes, expectation.cleanupExpectations)
}
```

- [ ] cleanup과 isolation mode validation helper를 구현한다. `validateMarker`, `requireUnique`, `assertCleanup`, invalid mode도 caller의 propagation/isolation diagnostic builder를 받아 동일한 safe coordinate format을 사용해야 하며 generic message로 우회하지 않는다.

```kotlin
private fun assertCleanup(
    actual: List<ContextCleanupProbe>,
    expected: List<ContextCleanupExpectation>,
) {
    val actualLocations = actual.map { it.location }
    val expectedLocations = expected.map { it.location }
    requireUnique(actualLocations, "cleanup observation locations")
    requireUnique(expectedLocations, "cleanup expectation locations")
    if (actualLocations.toSet() != expectedLocations.toSet()) {
        fail("Context cleanup location set mismatch")
    }
    expected.forEach { expectedProbe ->
        validateMarker(expectedProbe.expectedMarker, "cleanup expectation")
        val actualProbe = actual.single { it.location == expectedProbe.location }
        validateMarker(actualProbe.observedMarker, "cleanup observation")
        if (actualProbe.observedMarker != expectedProbe.expectedMarker) {
            fail("Context cleanup mismatch at ${expectedProbe.location}")
        }
    }
}

private fun validateIsolationExpectation(
    expectation: ContextIsolationSampleExpectation,
) {
    if (expectation.minimumObservationCount < 1) {
        fail("Context isolation minimum observation count must be positive")
    }
    expectation.expectedMarker?.let { validateMarker(it, "isolation expectation") }
    expectation.forbiddenMarkers.forEach {
        validateMarker(it, "isolation forbidden expectation")
    }
    requireUnique(expectation.forbiddenMarkers, "forbidden markers")

    val valid = when (expectation.mode) {
        ContextMarkerExpectationMode.EXACT ->
            expectation.expectedMarker != null && expectation.forbiddenMarkers.isEmpty()
        ContextMarkerExpectationMode.ABSENT ->
            expectation.expectedMarker == null && expectation.forbiddenMarkers.isEmpty()
        ContextMarkerExpectationMode.NOT_IN ->
            expectation.expectedMarker == null && expectation.forbiddenMarkers.isNotEmpty()
    }
    if (!valid) {
        fail("Context isolation expectation fields do not match ${expectation.mode}")
    }
}
```

- [ ] GREEN 명령:

```bash
./gradlew :bluetape4k-junit5:test \
  --tests "io.bluetape4k.junit5.observability.ContextPropagationConformanceTest"
```

**Expected GREEN:** 최소 10 tests pass.

- [ ] Lore commit:

```text
Define one provider-neutral proof language for context propagation

Constraint: Shared fixture must expose no framework-specific types or raw marker diagnostics
Rejected: Framework-specific assertion copies | they would drift in terminal and cleanup meaning
Confidence: high
Scope-risk: moderate
Directive: Keep future adapters responsible for collecting real framework evidence
Tested: :bluetape4k-junit5 targeted conformance test
Not-tested: Consumer adapters are added in later tasks
```

## Task 2: 공통 test-support helper와 resource lifecycle을 고정

**Files:**

- Create: `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/context/ContextPropagationTestSupport.kt`
- Create: `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/context/ContextPropagationConformanceTest.kt`

**Complexity:** M

**Dependencies:** Task 1

**Write scope:** 위 두 OpenTelemetry test 파일만

### Step 2.1 — RED: helper lifecycle의 failure-aware 계약 추가

- [ ] test class에 `@Timeout(... SAME_THREAD)`을 붙인다.
- [ ] 아직 없는 `runCoroutineScenario`, `runReactorScenario`, `runExecutorScenario`를 호출하는 3개 smoke test를 작성해 compile RED를 만든다.

```kotlin
@Timeout(
    value = 15,
    unit = TimeUnit.SECONDS,
    threadMode = Timeout.ThreadMode.SAME_THREAD,
)
class ContextPropagationConformanceTest {

    @Test
    fun `coroutine success propagates and restores context`() = runTest {
        val captured = runCoroutineScenario(ContextPropagationScenario.SUCCESS)
        captured.thrown.shouldBeNull()
        assertContextPropagationConformance(
            captured.observation,
            propagationExpectation(
                ContextPropagationBoundary.COROUTINE,
                ContextPropagationScenario.SUCCESS,
                ContextPropagationTerminal.SUCCESS,
            ),
        )
    }

    @Test
    fun `reactor success propagates and restores context`() {
        val captured = runReactorScenario(ContextPropagationScenario.SUCCESS)
        captured.thrown.shouldBeNull()
        assertContextPropagationConformance(
            captured.observation,
            propagationExpectation(
                ContextPropagationBoundary.REACTOR,
                ContextPropagationScenario.SUCCESS,
                ContextPropagationTerminal.SUCCESS,
            ),
        )
    }

    @Test
    fun `executor success propagates and restores context`() {
        val captured = runExecutorScenario(ContextPropagationScenario.SUCCESS)
        captured.thrown.shouldBeNull()
        assertContextPropagationConformance(
            captured.observation,
            propagationExpectation(
                ContextPropagationBoundary.TASK_EXECUTOR,
                ContextPropagationScenario.SUCCESS,
                ContextPropagationTerminal.SUCCESS,
            ),
        )
    }
}
```

- [ ] RED 명령:

```bash
./gradlew :bluetape4k-opentelemetry:test \
  --tests "io.bluetape4k.opentelemetry.context.ContextPropagationConformanceTest"
```

**Expected RED:** 세 adapter helper unresolved reference로 test compilation 실패.

### Step 2.2 — GREEN: test-only context key, snapshot builder, bounded resource helper 추가

```kotlin
internal val propagationMarkerKey: ContextKey<String> =
    ContextKey.named("bluetape4k-context-propagation-test-marker")

internal const val parentMarkerA = "parent-A"
internal const val parentMarkerB = "parent-B"

internal fun otelContext(marker: String): Context =
    Context.root().with(propagationMarkerKey, marker)

internal fun currentMarker(): String? =
    Context.current().get(propagationMarkerKey)

internal fun CountDownLatch.awaitOrFail(
    timeout: Duration = DEFAULT_CANCELLATION_CONTRACT_TIMEOUT,
) {
    check(await(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)) {
        "Timed out waiting for test gate"
    }
}

internal fun <T> Future<T>.getWithin(timeout: Duration): T =
    get(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)

internal enum class ConformanceEvent {
    READY,
    RELEASED,
    TERMINAL_OBSERVED,
    FINALLY_COMPLETED,
    CLEANUP_PROBED,
}

internal class ConformanceEventEntry(
    val requestAlias: ContextRequestAlias,
    val event: ConformanceEvent,
    val sequence: Long,
)

internal class ConformanceEventLedger {
    private val sequence = AtomicLong()
    private val events = ConcurrentLinkedQueue<ConformanceEventEntry>()

    fun record(
        requestAlias: ContextRequestAlias,
        event: ConformanceEvent,
    ) {
        events += ConformanceEventEntry(
            requestAlias,
            event,
            sequence.incrementAndGet(),
        )
    }

    fun assertIsolationOrder() {
        val snapshot = events.toList()
        val participantEntries = listOf(
            ContextRequestAlias.REQUEST_A,
            ContextRequestAlias.REQUEST_B,
        ).associateWith { alias ->
            snapshot.filter { it.requestAlias == alias }.also { entries ->
                val counts = entries.groupingBy { it.event }.eachCount()
                ConformanceEvent.entries.forEach { event ->
                    check(counts[event] == 1) {
                        "Lifecycle event count mismatch: alias=$alias, event=$event"
                    }
                }
            }
        }
        val lastReady = participantEntries.values
            .flatten()
            .filter { it.event == ConformanceEvent.READY }
            .maxOf { it.sequence }
        val firstRelease = participantEntries.values
            .flatten()
            .filter { it.event == ConformanceEvent.RELEASED }
            .minOf { it.sequence }
        check(lastReady < firstRelease)
        participantEntries.forEach { (alias, entries) ->
            assertCleanupAfterFinally(alias, entries)
        }
    }

    fun assertSingleScenarioOrder() {
        val entries = events.filter {
            it.requestAlias == ContextRequestAlias.SINGLE
        }
        val counts = entries.groupingBy { it.event }.eachCount()
        listOf(
            ConformanceEvent.TERMINAL_OBSERVED,
            ConformanceEvent.FINALLY_COMPLETED,
            ConformanceEvent.CLEANUP_PROBED,
        ).forEach { event ->
            check(counts[event] == 1) {
                "Lifecycle event count mismatch: alias=SINGLE, event=$event"
            }
        }
        check(counts[ConformanceEvent.READY] == null)
        check(counts[ConformanceEvent.RELEASED] == null)
        assertCleanupAfterFinally(ContextRequestAlias.SINGLE, entries)
    }

    private fun assertCleanupAfterFinally(
        alias: ContextRequestAlias,
        entries: List<ConformanceEventEntry>,
    ) {
        val finallySequence = entries.single {
            it.event == ConformanceEvent.FINALLY_COMPLETED
        }.sequence
        val terminalSequence = entries.single {
            it.event == ConformanceEvent.TERMINAL_OBSERVED
        }.sequence
        val cleanupSequence = entries.single {
            it.event == ConformanceEvent.CLEANUP_PROBED
        }.sequence
        check(terminalSequence < cleanupSequence) {
            "Lifecycle terminal cleanup order mismatch: alias=$alias"
        }
        check(finallySequence < cleanupSequence) {
            "Lifecycle cleanup order mismatch: alias=$alias"
        }
    }
}

internal fun propagationExpectation(
    boundary: ContextPropagationBoundary,
    scenario: ContextPropagationScenario,
    terminal: ContextPropagationTerminal,
    requestAlias: ContextRequestAlias = ContextRequestAlias.SINGLE,
): ContextPropagationExpectation =
    ContextPropagationExpectation(
        boundary = boundary,
        scenario = scenario,
        requestAlias = requestAlias,
        markerExpectations = listOf(
            ContextMarkerExpectation(ContextObservationPoint.BOUNDARY_ENTER, parentMarkerA),
            ContextMarkerExpectation(ContextObservationPoint.AFTER_SUSPENSION, parentMarkerA),
            ContextMarkerExpectation(ContextObservationPoint.BEFORE_TERMINAL, parentMarkerA),
        ),
        cleanupExpectations = listOf(
            ContextCleanupExpectation(ContextProbeLocation.CALLER, null),
            ContextCleanupExpectation(ContextProbeLocation.WORKER, null),
        ),
        expectedTerminal = terminal,
    )

internal fun ExecutorService.shutdownAndAssertTermination() {
    var interrupted: InterruptedException? = null
    var terminated = false
    try {
        shutdown()
        terminated = awaitTermination(5, TimeUnit.SECONDS)
    } catch (e: InterruptedException) {
        interrupted = e
    } finally {
        if (!terminated) {
            shutdownNow()
        }
    }

    if (!terminated) {
        terminated = try {
            awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            interrupted = interrupted ?: e
            false
        }
    }
    interrupted?.let {
        Thread.currentThread().interrupt()
        throw AssertionError("Interrupted while terminating test executor", it)
    }
    check(terminated) { "Test executor did not terminate" }
}
```

- [ ] scenario helper는 gate를 `finally`에서 항상 release하고, resource를 `use`/`try-finally`로 닫는다.
- [ ] interrupted teardown negative test를 추가해 `shutdownNow()` 실행과 interrupt status 복원을 고정한다.
- [ ] adapter가 던진 원래 failure/cancellation/deadline signal은 test boundary에서만 포착한다. adapter block은 terminal을 snapshot return으로 바꾸어 삼키지 않는다.

```kotlin
internal class CapturedScenario(
    val observation: ContextPropagationObservation,
    val thrown: Throwable?,
)

internal inline fun <reified T: Throwable> CapturedScenario.assertThrownExactly() {
    check(thrown?.javaClass == T::class.java) {
        "Scenario terminal type mismatch; values redacted"
    }
}
```

- [ ] scenario harness signatures를 고정한다. 각 harness만 adapter가 던진 signal을 test boundary에서 포착하고, `execute*` adapter helper는 원 signal을 재전파한다.

```kotlin
internal suspend fun runCoroutineScenario(
    scenario: ContextPropagationScenario,
): CapturedScenario

internal fun runReactorScenario(
    scenario: ContextPropagationScenario,
): CapturedScenario

internal fun runExecutorScenario(
    scenario: ContextPropagationScenario,
): CapturedScenario
```

- [ ] `CapturedScenario`는 test-private harness다. shared public fixture에 throwable/framework type을 추가하지 않는다.
- [ ] 이 task에서는 smoke test가 통과하는 최소 success path만 구현하며 Task 3–5가 terminal/isolation을 확장한다.

- [ ] GREEN 명령은 Step 2.1과 동일하다.

**Expected GREEN:** 3 smoke tests pass.

## Task 3: 실제 coroutine 경계 5개 scenario 구현

**Files:**

- Modify: `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/context/ContextPropagationTestSupport.kt`
- Modify: `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/context/ContextPropagationConformanceTest.kt`
- Reference: `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/ContextCoroutineSupport.kt`

**Complexity:** L

**Dependencies:** Task 2

**Write scope:** 두 OTel test 파일

### Step 3.1 — RED: failure/cancellation/deadline/isolation test 추가

```kotlin
@Test
fun `coroutine failure propagates and restores context`() = runTest {
    val captured = runCoroutineScenario(ContextPropagationScenario.FAILURE)
    captured.assertThrownExactly<IllegalStateException>()
    assertContextPropagationConformance(
        captured.observation,
        propagationExpectation(
            ContextPropagationBoundary.COROUTINE,
            ContextPropagationScenario.FAILURE,
            ContextPropagationTerminal.FAILURE,
        ),
    )
}

@Test
fun `coroutine cancellation propagates and restores context`() = runTest {
    val captured = runCoroutineScenario(ContextPropagationScenario.CANCELLATION)
    captured.assertThrownExactly<CancellationException>()
    assertContextPropagationConformance(
        captured.observation,
        propagationExpectation(
            ContextPropagationBoundary.COROUTINE,
            ContextPropagationScenario.CANCELLATION,
            ContextPropagationTerminal.CANCELLATION,
        ),
    )
}

@Test
fun `coroutine deadline propagates and restores context`() = runTest {
    val captured = runCoroutineScenario(ContextPropagationScenario.DEADLINE)
    captured.assertThrownExactly<TimeoutCancellationException>()
    assertContextPropagationConformance(
        captured.observation,
        propagationExpectation(
            ContextPropagationBoundary.COROUTINE,
            ContextPropagationScenario.DEADLINE,
            ContextPropagationTerminal.DEADLINE_EXCEEDED,
        ),
    )
}

@Test
fun `coroutine siblings keep isolated parent contexts`() = runTest {
    val observation = runCoroutineIsolationScenario()
    assertContextIsolation(observation, coroutineIsolationExpectation())
}
```

**Expected RED:** success-only helper가 terminal/isolation scenario를 지원하지 않아 assertions fail 또는 helper symbol unresolved.

### Step 3.2 — GREEN: 실제 `withOtelContext` 경계와 구조화된 cancellation 구현

- [ ] `Executors.newSingleThreadExecutor()`와 `asCoroutineDispatcher()`로 만든 dedicated dispatcher를 test가 소유한다. outer `finally`에서 child cancel/join → dispatcher close → backing executor `shutdownAndAssertTermination()` 순서를 실행한다.
- [ ] `withOtelContext(dispatcher, parent)`에서 enter/suspension 전후 값을 수집한다.
- [ ] `yield()`는 suspension point일 뿐 ordering 수단으로 사용하지 않는다.
- [ ] cancellation은 시작 gate 뒤 child cancel로 실제 `CancellationException`을 받고, deadline은 `withTimeout(semanticDeadline)` + `awaitCancellation()`으로 실제 `TimeoutCancellationException`을 받는다.
- [ ] adapter operation은 두 signal을 catch하지 않는다. test boundary만 signal identity/type을 먼저 assertion하고 별도로 보존한 observation에 shared assertion을 실행한다.
- [ ] A/B는 각각 child coroutine으로 실행하며 두 `CompletableDeferred<Unit>` gate를 교차 대기한다. `finally`에서 peer gate를 complete하고 child를 cancel/join한다.
- [ ] 종료 후 caller와 같은 dispatcher의 unwrapped worker probe를 수집한다.
- [ ] single scenario는 actual signal 관측, boundary `finally`, unwrapped probe 직후에 `ledger.record(SINGLE, TERMINAL_OBSERVED)`, `ledger.record(SINGLE, FINALLY_COMPLETED)`, `ledger.record(SINGLE, CLEANUP_PROBED)`를 정확히 한 번 호출하고 test가 `ledger.assertSingleScenarioOrder()`를 호출한다.
- [ ] isolation participant A/B는 gate 도달, 양쪽 ready 확인 후 release, terminal 관측, child `finally`, own-worker probe 직후에 각 alias로 `ledger.record(alias, READY/RELEASED/TERMINAL_OBSERVED/FINALLY_COMPLETED/CLEANUP_PROBED)`를 정확히 한 번 호출하고 test가 `ledger.assertIsolationOrder()`를 호출한다.

```kotlin
private suspend fun observeCoroutineBody(
    observations: MutableList<ContextMarkerObservation>,
) {
    observations += ContextMarkerObservation(
        ContextObservationPoint.BOUNDARY_ENTER,
        currentMarker(),
    )
    yield()
    observations += ContextMarkerObservation(
        ContextObservationPoint.AFTER_SUSPENSION,
        currentMarker(),
    )
    observations += ContextMarkerObservation(
        ContextObservationPoint.BEFORE_TERMINAL,
        currentMarker(),
    )
}

private suspend fun executeCoroutineTerminal(
    scenario: ContextPropagationScenario,
    observations: MutableList<ContextMarkerObservation>,
): Unit =
    withOtelContext(
        coroutineContext = testDispatcher,
        otelContext = otelContext(parentMarkerA),
    ) {
        observeCoroutineBody(observations)
        when (scenario) {
            ContextPropagationScenario.SUCCESS -> Unit
            ContextPropagationScenario.FAILURE -> error("synthetic failure")
            ContextPropagationScenario.CANCELLATION -> awaitCancellation()
            ContextPropagationScenario.DEADLINE ->
                withTimeout(semanticDeadline) { awaitCancellation() }
            ContextPropagationScenario.ISOLATION ->
                error("Isolation uses runCoroutineIsolationScenario")
        }
    }
```

- [ ] targeted 명령:

```bash
./gradlew :bluetape4k-opentelemetry:test \
  --tests "io.bluetape4k.opentelemetry.context.ContextPropagationConformanceTest"
```

**Expected GREEN:** coroutine 5 cases와 기존 smoke cases pass.

- [ ] Lore commit:

```text
Prove coroutine context restoration across every terminal path

Constraint: Cancellation and deadline must remain distinct and rethrow through cleanup
Rejected: Delay-based ordering | explicit gates make overlap and teardown deterministic
Confidence: high
Scope-risk: moderate
Directive: Preserve structured child ownership and bounded dispatcher termination
Tested: :bluetape4k-opentelemetry targeted conformance test
Not-tested: Reactor and executor terminal matrices are added next
```

## Task 4: 실제 Reactor subscriber 경계 5개 scenario 구현

**Files:**

- Modify: `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/context/ContextPropagationTestSupport.kt`
- Modify: `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/context/ContextPropagationConformanceTest.kt`

**Complexity:** L

**Dependencies:** Task 3

**Write scope:** 두 OTel test 파일

### Step 4.1 — RED: Reactor terminal matrix와 A/B subscriber isolation 추가

- [ ] success, `onError`, subscription `cancel`, Reactor `timeout`, A/B isolation test를 추가한다.
- [ ] cancellation test는 subscriber가 `BOUNDARY_ENTER`를 관측한 뒤 `Disposable.dispose()`한다.
- [ ] deadline test는 `Mono.never().timeout(semanticDeadline.toJavaDuration())`의 `TimeoutException`만 deadline으로 분류한다.

```kotlin
@Test
fun `reactor cancellation propagates and restores context`() {
    val captured = runReactorScenario(ContextPropagationScenario.CANCELLATION)
    captured.thrown.shouldBeNull()
    assertContextPropagationConformance(
        captured.observation,
        propagationExpectation(
            ContextPropagationBoundary.REACTOR,
            ContextPropagationScenario.CANCELLATION,
            ContextPropagationTerminal.CANCELLATION,
        ),
    )
}

@Test
fun `reactor subscribers keep isolated parent contexts`() {
    assertContextIsolation(
        runReactorIsolationScenario(),
        reactorIsolationExpectation(),
    )
}
```

**Expected RED:** Reactor helper가 success만 지원해 terminal/isolation assertion 실패.

### Step 4.2 — GREEN: subscriber-owned OTel context를 명시적으로 활성화

- [ ] test-private Reactor key에 captured OTel `Context`를 넣는다.
- [ ] `Mono.deferContextual`에서 해당 값을 읽고 callback을 `captured.wrap(...)`으로 실행한다.
- [ ] `doFinally`에서 `SignalType.CANCEL`, `ON_ERROR`, `ON_COMPLETE`를 실제 terminal로 기록한다.
- [ ] Reactor cancellation은 throwable을 발명하지 않는다. `CapturedScenario.thrown == null`과 실제 `SignalType.CANCEL`에서 만든 observation terminal을 함께 검증한다.
- [ ] 단일 scenario는 test-owned `Schedulers.newSingle`에 `subscribeOn`하여 실제 worker 경계를 통과한다.
- [ ] A/B isolation은 각각 별도 test-owned single scheduler를 사용한다. 각 publisher는 `Sinks.One<Unit>` ready signal을 먼저 emit하고 cached `Mono.when(readyA.asMono(), readyB.asMono())`를 non-blocking으로 기다린 뒤 두 번째 관측을 수행한다. 같은 single worker에서 blocking latch/barrier await를 금지한다.
- [ ] cached shared gate 이후 각 participant는 `publishOn(ownScheduler)`로 자신의 rail에 다시 고정한 다음 두 번째 marker를 관측한다. test-owned scheduler name A/B를 gate 전후에 확인해 continuation이 peer scheduler에서 실행된 false isolation proof를 차단한다.
- [ ] parent helper가 두 `Disposable`을 모두 소유한다. 첫 failure를 atomic holder에 보존하고 모든 completion wait에 `hangGuard`를 적용하며, outer `finally`에서 ready sink 종료 → 두 subscription dispose → bounded completion 확인 순서를 보장한다.
- [ ] 각 subscriber 종료 후 자신이 사용한 같은 scheduler에 unwrapped context probe를 순차 제출한다.
- [ ] 각 scheduler는 `disposeGracefully().block(hangGuard)` 뒤 `isDisposed`를 assertion한다.
- [ ] single subscriber는 `doFinally` actual signal, subscriber finalizer, same-scheduler probe 뒤 `ledger.record(SINGLE, TERMINAL_OBSERVED/FINALLY_COMPLETED/CLEANUP_PROBED)`를 정확히 한 번 호출하고 `ledger.assertSingleScenarioOrder()`를 호출한다.
- [ ] A/B subscriber는 ready sink emit, cached both-ready 완료 후 own-rail release, actual terminal, `doFinally`, same-rail probe 순서로 `ledger.record(alias, READY/RELEASED/TERMINAL_OBSERVED/FINALLY_COMPLETED/CLEANUP_PROBED)`를 호출하고 `ledger.assertIsolationOrder()`를 호출한다.

```kotlin
private const val reactorOtelContextKey = "bluetape4k.otel.context"

private fun observedMono(
    observations: MutableList<ContextMarkerObservation>,
    testScheduler: Scheduler,
): Mono<Unit> =
    Mono.deferContextual { view ->
        val captured = view.get<Context>(reactorOtelContextKey)
        Mono.fromCallable(
            captured.wrap(
                Callable {
                    observations += ContextMarkerObservation(
                        ContextObservationPoint.BOUNDARY_ENTER,
                        currentMarker(),
                    )
                    observations += ContextMarkerObservation(
                        ContextObservationPoint.AFTER_SUSPENSION,
                        currentMarker(),
                    )
                    observations += ContextMarkerObservation(
                        ContextObservationPoint.BEFORE_TERMINAL,
                        currentMarker(),
                    )
                    Unit
                },
            ),
        )
    }
        .contextWrite { it.put(reactorOtelContextKey, otelContext(parentMarkerA)) }
        .subscribeOn(testScheduler)

private fun workerProbe(scheduler: Scheduler): String? =
    Mono.fromCallable(::currentMarker)
        .subscribeOn(scheduler)
        .block(hangGuard.toJavaDuration())
```

```kotlin
private fun afterSharedGate(
    bothReady: Mono<Void>,
    ownScheduler: Scheduler,
    schedulerPrefix: String,
    observe: () -> Unit,
): Mono<Void> =
    bothReady
        .publishOn(ownScheduler)
        .doOnSuccess {
            check(Thread.currentThread().name.startsWith(schedulerPrefix))
            observe()
        }
```

- [ ] targeted 명령은 Task 3과 동일하다.

**Expected GREEN:** coroutine 5 + Reactor 5 + executor success smoke pass.

- [ ] Lore commit:

```text
Prove subscriber context without relying on global Reactor hooks

Constraint: Reactor Context and OpenTelemetry current context are separate stores
Rejected: Automatic global propagation hooks | they introduce JVM-wide order dependence
Confidence: high
Scope-risk: moderate
Directive: Keep activation scoped to each subscriber callback
Tested: :bluetape4k-opentelemetry targeted conformance test
Not-tested: Executor terminal matrix remains
```

## Task 5: 실제 single-thread executor 경계 5개 scenario 구현

**Files:**

- Modify: `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/context/ContextPropagationTestSupport.kt`
- Modify: `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/context/ContextPropagationConformanceTest.kt`

**Complexity:** L

**Dependencies:** Task 4

**Write scope:** 두 OTel test 파일

### Step 5.1 — RED: running cancellation, `Future.get` deadline, same-worker isolation 추가

- [ ] success, task exception, running `Future.cancel(true)`, `Future.getWithin(semanticDeadline)` timeout 뒤 cancel, same worker A/B를 검증한다.
- [ ] cancellation/deadline은 `entered` gate 전에 cancel하지 않는다.
- [ ] worker probe는 `finallyCompleted` gate 뒤에만 제출한다.

```kotlin
@Test
fun `executor deadline cancels running work and restores worker context`() {
    val captured = runExecutorScenario(ContextPropagationScenario.DEADLINE)
    captured.assertThrownExactly<TimeoutException>()
    assertContextPropagationConformance(
        captured.observation,
        propagationExpectation(
            ContextPropagationBoundary.TASK_EXECUTOR,
            ContextPropagationScenario.DEADLINE,
            ContextPropagationTerminal.DEADLINE_EXCEEDED,
        ),
    )
}

@Test
fun `executor reuses one worker without leaking request context`() {
    assertContextIsolation(
        runExecutorIsolationScenario(),
        executorIsolationExpectation(),
    )
}
```

**Expected RED:** executor helper가 success-only라 terminal/isolation assertion 실패.

### Step 5.2 — GREEN: submission-time `Context.wrap`와 finally handshake 구현

```kotlin
private fun submitWrapped(
    executor: ExecutorService,
    marker: String,
    entered: CountDownLatch,
    finallyCompleted: CountDownLatch,
    body: () -> Unit,
): Future<Unit> =
    executor.submit(
        otelContext(marker).wrap(
            Callable {
                try {
                    entered.countDown()
                    body()
                    Unit
                } finally {
                    finallyCompleted.countDown()
                }
            },
        ),
    )
```

- [ ] 일반 예외는 `ExecutionException.cause`가 synthetic `IllegalStateException`인지 확인하고 `FAILURE`로 분류한다.
- [ ] cancellation은 `entered.awaitOrFail(hangGuard)` 뒤 `future.cancel(true)`가 true인지 확인한다.
- [ ] deadline은 `future.getWithin(semanticDeadline)`이 `TimeoutException`인지 확인한 뒤 cancel한다.
- [ ] cancellation/deadline task body는 interrupt를 확인하고 `finally`로 빠져나온다. interrupt를 삼키지 않는다.
- [ ] 모든 terminal에서 `finallyCompleted.awaitOrFail(hangGuard)`를 확인한다.
- [ ] 동일 executor의 unwrapped `Callable(::currentMarker)` probe가 `null`인지 수집한다.
- [ ] A task → unwrapped probe → B task → unwrapped probe 순서로 같은 worker를 재사용하고 각 task 내부에서 두 번 이상 marker를 관측한다.
- [ ] outer `finally`에서 outstanding future cancel, gate release, `shutdownAndAssertTermination()`을 수행한다.
- [ ] single task는 `Future` terminal 관측, task `finallyCompleted`, same-worker probe 순서로 `ledger.record(SINGLE, TERMINAL_OBSERVED/FINALLY_COMPLETED/CLEANUP_PROBED)`를 호출한 뒤 `ledger.assertSingleScenarioOrder()`를 호출한다.
- [ ] A/B task는 entered/ready, 양쪽 ready 뒤 release, `Future` terminal, task `finally`, same-worker unwrapped probe마다 `ledger.record(alias, READY/RELEASED/TERMINAL_OBSERVED/FINALLY_COMPLETED/CLEANUP_PROBED)`를 호출하고 `ledger.assertIsolationOrder()`를 호출한다.

- [ ] targeted 명령:

```bash
./gradlew :bluetape4k-opentelemetry:test \
  --tests "io.bluetape4k.opentelemetry.context.ContextPropagationConformanceTest"
```

**Expected GREEN:** 정확히 또는 최소 15 OTel conformance cases pass.

- [ ] Lore commit:

```text
Prove submission-time context capture and same-worker restoration

Constraint: Cleanup probes must run only after the cancelled task completes finally
Rejected: Probe immediately after Future.cancel | it races the scope restoration
Confidence: high
Scope-risk: moderate
Directive: Keep entered-cancelled-finally handshakes explicit
Tested: :bluetape4k-opentelemetry targeted conformance test
Not-tested: Spring and Ktor adapters remain
```

## Task 6: Spring suspending observation adapter 5개 scenario 구현

**Files:**

- Create: `spring-boot/core/src/test/kotlin/io/bluetape4k/spring/observability/SpringContextPropagationConformanceTest.kt`
- Reference: `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/observability/SpringObservationSupport.kt`
- Reference: `spring-boot/core/src/test/kotlin/io/bluetape4k/spring/observability/SpringObservationSupportTest.kt`

**Complexity:** L

**Dependencies:** Task 1

**Write scope:** 새 Spring test 파일만

### Step 6.1 — RED: 공통 assertion을 호출하는 5개 Spring test 추가

- [ ] success, block exception, child cancellation, `withTimeout` deadline, A/B child isolation을 추가한다.
- [ ] `TestObservationRegistry.create()`와 고정 synthetic observation name을 사용한다.
- [ ] 각 test 종료 후 `registry.currentObservation == null`을 probe로 수집한다.
- [ ] 이 파일의 private regular `CapturedScenario`가 observation과 원 throwable을 보존한다. OTel test-support type을 import하거나 public fixture를 확장하지 않는다.
- [ ] `private suspend fun runSpringScenario(scenario: ContextPropagationScenario): CapturedScenario` signature를 사용한다.
- [ ] OTel test-support와 동일한 alias-keyed event-ledger shape를 이 test 파일에 private regular class로 둔다. shared public fixture에는 lifecycle implementation type을 추가하지 않는다.

```kotlin
@Test
fun `spring observation is visible across suspension and cleaned on success`() = runTest {
    val captured = runSpringScenario(ContextPropagationScenario.SUCCESS)
    captured.thrown.shouldBeNull()
    assertContextPropagationConformance(
        captured.observation,
        springExpectation(
            ContextPropagationScenario.SUCCESS,
            ContextPropagationTerminal.SUCCESS,
        ),
    )
}

@Test
fun `spring observations stay isolated between sibling coroutines`() = runTest {
    assertContextIsolation(
        runSpringIsolationScenario(),
        springIsolationExpectation(),
    )
}
```

- [ ] RED 명령:

```bash
./gradlew :bluetape4k-spring-boot-core:test \
  --tests "io.bluetape4k.spring.observability.SpringContextPropagationConformanceTest"
```

**Expected RED:** adapter helper unresolved reference로 test compilation 실패.

### Step 6.2 — GREEN: 실제 `observeSpringSuspending` lifecycle 관측

- [ ] setup 순서를 registry → observation marker → adapter call → suspension 관측 → terminal capture → caller/registry probe로 고정한다.
- [ ] observation marker는 `Observation.currentObservation.name`에서 읽고 raw value를 failure message에 넣지 않는다.
- [ ] cancellation은 started gate 뒤 child cancel/join으로 발생시킨다.
- [ ] deadline은 `withTimeout(semanticDeadline) { awaitCancellation() }`을 adapter block 안에서 실행한다.
- [ ] A/B는 각자 별도 registry와 marker를 사용하고 failure-aware `CompletableDeferred` barrier로 중첩한다. parent scope가 두 child를 소유하고 모든 await에 `hangGuard`를 적용하며 outer `finally`에서 peer gate complete/cancel → sibling cancel → 두 child join 순서를 보장한다.
- [ ] `CancellationException`과 `TimeoutCancellationException`을 test boundary에서 구분한다. `observeSpringSuspending`와 adapter helper는 cleanup 후 원래 signal을 그대로 재전파한다.
- [ ] single observation은 actual throwable/result 관측, `observeSpringSuspending` 종료 `finally`, registry cleanup probe 뒤 `ledger.record(SINGLE, TERMINAL_OBSERVED/FINALLY_COMPLETED/CLEANUP_PROBED)`를 호출하고 `ledger.assertSingleScenarioOrder()`를 호출한다.
- [ ] A/B child는 ready, 양쪽 ready 뒤 release, terminal, child `finally`, 각 registry cleanup probe마다 `ledger.record(alias, READY/RELEASED/TERMINAL_OBSERVED/FINALLY_COMPLETED/CLEANUP_PROBED)`를 호출하고 `ledger.assertIsolationOrder()`를 호출한다.

```kotlin
private fun ObservationRegistry.currentMarker(): String? =
    currentObservation?.context?.name

private suspend fun ObservationRegistry.observeMarkers(
    marker: String,
    observations: MutableList<ContextMarkerObservation>,
    terminalAction: suspend () -> Unit,
) {
    observeSpringSuspending(
        name = marker,
    ) {
        observations += ContextMarkerObservation(
            ContextObservationPoint.BOUNDARY_ENTER,
            currentMarker(),
        )
        yield()
        observations += ContextMarkerObservation(
            ContextObservationPoint.AFTER_SUSPENSION,
            currentMarker(),
        )
        observations += ContextMarkerObservation(
            ContextObservationPoint.BEFORE_TERMINAL,
            currentMarker(),
        )
        terminalAction()
    }
}
```

- [ ] GREEN 명령은 Step 6.1과 동일하다.

**Expected GREEN:** 최소 5 Spring conformance cases pass.

- [ ] Lore commit:

```text
Bind Spring observation lifecycle to the shared propagation contract

Constraint: The adapter must exercise observeSpringSuspending rather than a test-only scope
Rejected: Shared Spring-specific fixture | framework interception belongs in its module
Confidence: high
Scope-risk: moderate
Directive: Keep registry cleanup checks on every terminal path
Tested: :bluetape4k-spring-boot-core targeted conformance test
Not-tested: Ktor request adapter remains
```

## Task 7: Ktor W3C request adapter 5개 scenario 구현

**Files:**

- Create: `ktor/observability/src/test/kotlin/io/bluetape4k/ktor/observability/KtorContextPropagationConformanceTest.kt`
- Reference: `ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/KtorOpenTelemetryTracingSupport.kt`
- Reference: `ktor/observability/src/test/kotlin/io/bluetape4k/ktor/observability/Bluetape4kKtorObservabilityTest.kt`

**Complexity:** XL

**Dependencies:** Task 1

**Write scope:** 새 Ktor test 파일만

### Step 7.1 — RED: in-process request matrix와 A/B/probe isolation 추가

- [ ] success, handler exception, request coroutine cancel, route `withTimeout`, concurrent A/B + unparented probe를 추가한다.
- [ ] caller가 observation에서 얻은 marker로 expectation을 역생성하지 않는다. A/B trace ID는 사전 고정한 synthetic 값이다.
- [ ] 이 파일의 private regular `CapturedScenario`가 observation과 원 throwable을 보존한다. OTel/Spring test-private type을 공유하지 않는다.
- [ ] `private suspend fun ApplicationTestBuilder.runKtorScenario(tracing: TestTracing, scenario: ContextPropagationScenario): CapturedScenario` signature를 사용해 `testApplication`의 in-process `client`에 접근한다.
- [ ] isolation helper도 `private suspend fun ApplicationTestBuilder.runKtorIsolationScenario(tracing: TestTracing): ContextIsolationObservation` receiver extension으로 고정한다. 일반 멤버 함수가 DSL receiver를 암묵적으로 상속한다고 가정하지 않는다.
- [ ] 동일한 alias-keyed event-ledger shape를 이 test 파일에 private regular class로 둔다. 다른 module의 test-private type을 import하지 않는다.

```kotlin
@Test
fun `ktor extracts synthetic W3C parent and cleans request on success`() =
    TestTracing().use { fixture ->
        testApplication {
            application {
                installBluetape4kKtorOpenTelemetryTracing(
                    KtorOpenTelemetryTracingConfig(
                        openTelemetry = fixture.openTelemetry,
                    ),
                )
                contextRoutes()
            }
            val captured =
                runKtorScenario(fixture, ContextPropagationScenario.SUCCESS)
            captured.thrown.shouldBeNull()
            assertContextPropagationConformance(
                captured.observation,
                ktorExpectation(
                    ContextPropagationScenario.SUCCESS,
                    ContextPropagationTerminal.SUCCESS,
                ),
            )
        }
    }

@Test
fun `ktor concurrent parents and unparented probe stay isolated`() =
    TestTracing().use { fixture ->
        testApplication {
            application {
                installBluetape4kKtorOpenTelemetryTracing(
                    KtorOpenTelemetryTracingConfig(
                        openTelemetry = fixture.openTelemetry,
                    ),
                )
                contextRoutes()
            }
            assertContextIsolation(
                runKtorIsolationScenario(fixture),
                ktorIsolationExpectation(),
            )
        }
    }
```

- [ ] RED 명령:

```bash
./gradlew :bluetape4k-ktor-observability:test \
  --tests "io.bluetape4k.ktor.observability.KtorContextPropagationConformanceTest"
```

**Expected RED:** Ktor test adapter/fixture unresolved reference로 compilation 실패.

### Step 7.2 — GREEN: local SDK + W3C propagator + deterministic route 구현

- [ ] `OpenTelemetrySdk`에 `W3CTraceContextPropagator.getInstance()`를 명시적으로 등록한다.
- [ ] fixed valid trace/span ID A/B로 `SpanContext.create(...)`와 `Span.wrap(...)`을 만든다.
- [ ] text-map propagator가 mutable map에 header를 inject하고 Ktor client request가 이를 전송한다.
- [ ] handler는 `Span.current().spanContext.traceId`를 세 point에서 관측한다.
- [ ] concurrent A/B request는 channel/deferred barrier로 실제 overlap시키고 route `finally`에서 peer를 해제한다.
- [ ] parent test scope가 두 client request job을 소유한다. 모든 gate wait는 `hangGuard`로 감싸고 첫 failure를 보존하며 outer `finally`에서 peer gate release → 두 request cancel → 두 job join 순서를 보장한다.
- [ ] unparented probe request의 current trace ID가 non-null이고 A/B forbidden list에 없음을 `NOT_IN`으로 검증한다.
- [ ] request cancellation은 handler가 started gate를 연 뒤 자신의 `coroutineContext.job`을 synthetic `CancellationException`으로 cancel하고 다음 suspension에서 실제 signal을 받게 한다. parent는 client request job을 bounded join한다. route deadline은 250ms `withTimeout`이 실제로 발생해야 한다.
- [ ] local SDK/provider/exporter는 `AutoCloseable` fixture가 bounded provider shutdown을 검증하며 닫는다. `SimpleSpanProcessor`는 synchronous export이므로 중복 `forceFlush`를 수행하지 않는다.
- [ ] `SdkTracerProvider.shutdown()`이 owned span processor/exporter teardown의 authoritative bounded result다. 이후 `OpenTelemetrySdk.close()`로 같은 provider를 중복 shutdown하지 않는다.
- [ ] ownership은 outer `TestTracing().use { testApplication { ... } }`로 고정한다. request completion/cancel → `testApplication` application teardown → provider shutdown 순서를 바꾸지 않는다.
- [ ] single request는 response/throwable terminal 관측, route `finally`, request-completion probe 뒤 `ledger.record(SINGLE, TERMINAL_OBSERVED/FINALLY_COMPLETED/CLEANUP_PROBED)`를 호출하고 `ledger.assertSingleScenarioOrder()`를 호출한다.
- [ ] A/B request는 handler ready, 양쪽 ready 뒤 release, client terminal, route `finally`, 각 request completion마다 `ledger.record(alias, READY/RELEASED/TERMINAL_OBSERVED/FINALLY_COMPLETED/CLEANUP_PROBED)`를 호출하고 `ledger.assertIsolationOrder()`를 호출한다. unparented `PROBE` request는 isolation sample에는 포함하지만 A/B lifecycle count에는 섞지 않는다.

```kotlin
private const val traceIdA = "11111111111111111111111111111111"
private const val spanIdA = "1111111111111111"
private const val traceIdB = "22222222222222222222222222222222"
private const val spanIdB = "2222222222222222"

private class TestTracing : AutoCloseable {
    val spanExporter: InMemorySpanExporter = InMemorySpanExporter.create()
    private val tracerProvider: SdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build()

    val openTelemetry: OpenTelemetrySdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(
                ContextPropagators.create(W3CTraceContextPropagator.getInstance()),
            )
            .build()

    fun headers(traceId: String, spanId: String): Map<String, String> {
        val carrier = mutableMapOf<String, String>()
        val parent = Span.wrap(
            SpanContext.create(
                traceId,
                spanId,
                TraceFlags.getSampled(),
                TraceState.getDefault(),
            ),
        )
        openTelemetry.propagators.textMapPropagator.inject(
            Context.root().with(parent),
            carrier,
        ) { target, key, value ->
            target?.set(key, value)
        }
        return carrier
    }

    override fun close() {
        check(
            tracerProvider.shutdown()
                .join(5, TimeUnit.SECONDS)
                .isSuccess,
        ) {
            "Test tracer provider shutdown failed"
        }
    }
}
```

```kotlin
private fun Application.contextRoutes() {
    routing {
        get("/context/{scenario}") {
            val scenario = ContextPropagationScenario.valueOf(
                requireNotNull(call.parameters["scenario"]),
            )
            val traceId = Span.current().spanContext.traceId
            record(
                scenario,
                ContextObservationPoint.BOUNDARY_ENTER,
                traceId,
            )
            yield()
            record(
                scenario,
                ContextObservationPoint.AFTER_SUSPENSION,
                Span.current().spanContext.traceId,
            )
            record(
                scenario,
                ContextObservationPoint.BEFORE_TERMINAL,
                Span.current().spanContext.traceId,
            )
            when (scenario) {
                ContextPropagationScenario.SUCCESS -> call.respond(HttpStatusCode.OK)
                ContextPropagationScenario.FAILURE -> error("synthetic handler failure")
                ContextPropagationScenario.CANCELLATION -> {
                    coroutineContext.job.cancel(
                        CancellationException("synthetic request cancellation"),
                    )
                    yield()
                }
                ContextPropagationScenario.DEADLINE ->
                    withTimeout(semanticDeadline) { awaitCancellation() }
                ContextPropagationScenario.ISOLATION -> runIsolationBarrier(call)
            }
        }
    }
}
```

- [ ] GREEN 명령은 Step 7.1과 동일하다.

**Expected GREEN:** 최소 5 Ktor conformance cases pass, 외부 port/process 없음.

- [ ] Lore commit:

```text
Prove request-scoped W3C parent extraction without external telemetry

Constraint: Request isolation must use fixed expectations and an unparented probe
Rejected: Global OpenTelemetry registration | local SDK ownership prevents suite pollution
Confidence: high
Scope-risk: moderate
Directive: Keep A B overlap failure-aware and close every local SDK
Tested: :bluetape4k-ktor-observability targeted conformance test
Not-tested: Full four-module and detekt validation remain
```

## Task 8: bilingual docs, full validation, review, delivery

**Files:**

- Modify: `testing/junit5/README.md`
- Modify: `testing/junit5/README.ko.md`
- Create: `docs/lessons/2026-07-28-issue-1051-context-propagation-conformance.md`
- Review: all files changed since `origin/develop`

**Complexity:** M

**Dependencies:** Tasks 1–7

**Write scope:** README pair, lesson, review fixes in already-touched files

### Step 8.1 — docs-first/API example verification

- [ ] README pair에 동일한 구조로 다음을 기록한다.
  - provider-neutral fixture의 목적
  - propagation과 isolation minimal example
  - `null` root, terminal 구분, raw marker redaction
  - test-owned synthetic marker만 허용하고 production request ID, user data, external trace ID를 금지
  - `Serializable` snapshot은 persistence/wire contract가 아니며 장기 저장 금지
  - enum value 추가는 additive이므로 caller exhaustive `when`에는 `else` 사용
  - data-class constructor 변경은 compatibility-sensitive이므로 구조분해/저장 포맷 의존 금지
  - adapter가 실제 framework context를 snapshot으로 변환해야 한다는 책임
- [ ] 예제는 실제 package/import/function 이름만 사용한다. Task 1 self-test의 `README propagation example compiles`와 `README isolation example compiles`가 같은 object construction과 assertion 호출을 compile/run하며, README pair는 그 검증된 snippet을 그대로 복사한다.

```kotlin
import io.bluetape4k.junit5.observability.*

val marker = "synthetic-parent"
val observation = ContextPropagationObservation(
    boundary = ContextPropagationBoundary.COROUTINE,
    scenario = ContextPropagationScenario.SUCCESS,
    requestAlias = ContextRequestAlias.SINGLE,
    markerObservations = listOf(
        ContextMarkerObservation(
            ContextObservationPoint.BOUNDARY_ENTER,
            marker,
        ),
        ContextMarkerObservation(
            ContextObservationPoint.AFTER_SUSPENSION,
            marker,
        ),
        ContextMarkerObservation(
            ContextObservationPoint.BEFORE_TERMINAL,
            marker,
        ),
    ),
    cleanupProbes = listOf(
        ContextCleanupProbe(ContextProbeLocation.CALLER, null),
    ),
    terminal = ContextPropagationTerminal.SUCCESS,
)
val expectation = ContextPropagationExpectation(
    boundary = ContextPropagationBoundary.COROUTINE,
    scenario = ContextPropagationScenario.SUCCESS,
    requestAlias = ContextRequestAlias.SINGLE,
    markerExpectations = listOf(
        ContextMarkerExpectation(
            ContextObservationPoint.BOUNDARY_ENTER,
            marker,
        ),
        ContextMarkerExpectation(
            ContextObservationPoint.AFTER_SUSPENSION,
            marker,
        ),
        ContextMarkerExpectation(
            ContextObservationPoint.BEFORE_TERMINAL,
            marker,
        ),
    ),
    cleanupExpectations = listOf(
        ContextCleanupExpectation(ContextProbeLocation.CALLER, null),
    ),
    expectedTerminal = ContextPropagationTerminal.SUCCESS,
)

assertContextPropagationConformance(observation, expectation)
```

```kotlin
import io.bluetape4k.junit5.observability.*

val isolationObservation = ContextIsolationObservation(
    boundary = ContextPropagationBoundary.KTOR_REQUEST,
    samples = listOf(
        ContextIsolationSample(
            ContextRequestAlias.REQUEST_A,
            listOf("synthetic-parent-A", "synthetic-parent-A"),
        ),
        ContextIsolationSample(
            ContextRequestAlias.REQUEST_B,
            listOf("synthetic-parent-B", "synthetic-parent-B"),
        ),
        ContextIsolationSample(
            ContextRequestAlias.PROBE,
            listOf("synthetic-probe"),
        ),
    ),
    cleanupProbes = listOf(
        ContextCleanupProbe(ContextProbeLocation.REQUEST, null),
    ),
)
val isolationExpectation = ContextIsolationExpectation(
    boundary = ContextPropagationBoundary.KTOR_REQUEST,
    samples = listOf(
        ContextIsolationSampleExpectation(
            requestAlias = ContextRequestAlias.REQUEST_A,
            mode = ContextMarkerExpectationMode.EXACT,
            expectedMarker = "synthetic-parent-A",
            minimumObservationCount = 2,
        ),
        ContextIsolationSampleExpectation(
            requestAlias = ContextRequestAlias.REQUEST_B,
            mode = ContextMarkerExpectationMode.EXACT,
            expectedMarker = "synthetic-parent-B",
            minimumObservationCount = 2,
        ),
        ContextIsolationSampleExpectation(
            requestAlias = ContextRequestAlias.PROBE,
            mode = ContextMarkerExpectationMode.NOT_IN,
            forbiddenMarkers = listOf(
                "synthetic-parent-A",
                "synthetic-parent-B",
            ),
        ),
    ),
    cleanupExpectations = listOf(
        ContextCleanupExpectation(ContextProbeLocation.REQUEST, null),
    ),
)

assertContextIsolation(isolationObservation, isolationExpectation)
```

- [ ] lesson은 한국어로 문제, 선택한 shared boundary, cancellation/deadline 구분, deterministic barrier, production 비변경, 검증 evidence를 기록한다.

### Step 8.2 — 전체 validation

- [ ] 각 module suite를 순차 실행한다. Testcontainers-backed 작업과 병렬 실행하지 않는다.

```bash
/usr/bin/time -p ./gradlew :bluetape4k-junit5:test
/usr/bin/time -p ./gradlew :bluetape4k-opentelemetry:test
/usr/bin/time -p ./gradlew :bluetape4k-spring-boot-core:test
/usr/bin/time -p ./gradlew :bluetape4k-ktor-observability:test
/usr/bin/time -p ./gradlew detekt
git diff --check
```

**Expected GREEN:** 네 module full suite의 event-ledger partial-order assertions, detekt success, whitespace error 없음.

- [ ] `/usr/bin/time -p`의 네 full module suite elapsed time을 lesson evidence에 기록한다. targeted 명령은 구현 중 RED/GREEN feedback에만 사용하고 최종 evidence로 full suite와 중복 요구하지 않는다.
- [ ] `repo-diff`로 production/build/module-registration 변경이 없음을 확인한다.
- [ ] fresh full-suite JUnit XML에서 최소 case 수와 단일 case runtime을 executable gate로 확인한다. XML이 없거나 어느 conformance case가 7.5초 이상이면 실패한다.
  - shared fixture ≥10
  - OTel ≥15
  - Spring ≥5
  - Ktor ≥5

```bash
python3 - <<'PY'
from pathlib import Path
from xml.etree import ElementTree

expected = {
    "testing/junit5/build/test-results/test": (
        "ContextPropagationConformanceTest",
        10,
    ),
    "infra/opentelemetry/build/test-results/test": (
        "ContextPropagationConformanceTest",
        15,
    ),
    "spring-boot/core/build/test-results/test": (
        "SpringContextPropagationConformanceTest",
        5,
    ),
    "ktor/observability/build/test-results/test": (
        "KtorContextPropagationConformanceTest",
        5,
    ),
}
for directory, (class_name, minimum) in expected.items():
    reports = sorted(Path(directory).glob(f"TEST-*{class_name}*.xml"))
    if not reports:
        raise SystemExit(f"missing JUnit XML for {directory}/{class_name}")
    count = sum(
        int(ElementTree.parse(path).getroot().attrib["tests"])
        for path in reports
    )
    if count < minimum:
        raise SystemExit(
            f"insufficient cases for {directory}/{class_name}: {count} < {minimum}"
        )
    durations = [
        float(case.attrib.get("time", "0"))
        for path in reports
        for case in ElementTree.parse(path).getroot().iter("testcase")
    ]
    slowest = max(durations, default=0.0)
    if slowest >= 7.5:
        raise SystemExit(
            f"slow conformance case for {directory}/{class_name}: {slowest}s"
        )
    print(
        f"{directory}/{class_name}: {count} >= {minimum}, "
        f"slowest={slowest:.3f}s < 7.5s"
    )
PY
```

- [ ] touched files에서 global telemetry/context mutation과 raw output API를 사용하지 않았음을 실패형 grep으로 확인한다.

```bash
if rg -n \
  "GlobalOpenTelemetry\\.(set|resetForTest)|Hooks\\.|enableAutomaticContextPropagation|println\\(|printStackTrace\\(" \
  testing/junit5/src/main/kotlin/io/bluetape4k/junit5/observability/ContextPropagationConformance.kt \
  testing/junit5/src/test/kotlin/io/bluetape4k/junit5/observability/ContextPropagationConformanceTest.kt \
  infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/context \
  spring-boot/core/src/test/kotlin/io/bluetape4k/spring/observability/SpringContextPropagationConformanceTest.kt \
  ktor/observability/src/test/kotlin/io/bluetape4k/ktor/observability/KtorContextPropagationConformanceTest.kt; then
  exit 1
fi
```

**Expected GREEN:** grep output 없음, exit 0.

- [ ] README English/Korean parity를 executable gate로 확인한다.

```bash
python3 - <<'PY'
from pathlib import Path

english = Path("testing/junit5/README.md").read_text()
korean = Path("testing/junit5/README.ko.md").read_text()
symbols = [
    "ContextPropagationObservation",
    "ContextPropagationExpectation",
    "ContextIsolationObservation",
    "ContextIsolationExpectation",
    "assertContextPropagationConformance",
    "assertContextIsolation",
    "ContextMarkerExpectationMode.EXACT",
    "ContextMarkerExpectationMode.NOT_IN",
]
for symbol in symbols:
    if symbol not in english or symbol not in korean:
        raise SystemExit(f"README parity missing symbol: {symbol}")
if english.count("```kotlin") < 2 or korean.count("```kotlin") < 2:
    raise SystemExit("README parity requires propagation and isolation examples")
required = {
    "English": (
        english,
        ["synthetic marker", "values redacted", "Serializable", "exhaustive when"],
    ),
    "Korean": (
        korean,
        ["synthetic marker", "값 비공개", "Serializable", "exhaustive when"],
    ),
}
for label, (text, phrases) in required.items():
    missing = [phrase for phrase in phrases if phrase not in text]
    if missing:
        raise SystemExit(f"{label} README parity missing: {missing}")
print("README EN/KO context propagation parity verified")
PY
```

- [ ] parity script와 두 compile-tested README example self-test가 모두 green이어야 docs gate를 통과한다.
- [ ] current-session pre-PR six-tier review를 실행하고 P0/P1=0으로 수렴한다.
- [ ] review finding이 production code/dependency 변경을 요구하면 구현을 멈추고 spec을 다시 연다.

### Step 8.3 — 최종 Lore commit

```text
Document the reusable context propagation proof boundary

Constraint: Public examples must remain framework-neutral and bilingual
Rejected: Per-adapter duplicated guidance | it would drift from the shared contract
Confidence: high
Scope-risk: narrow
Directive: Keep docs synchronized when the fixture surface changes
Tested: Four module suites, detekt, git diff --check
Not-tested: No external collector or server is in scope
```

### Step 8.4 — PR metadata와 stop condition

- [ ] branch push 전 `git status`, `git log origin/develop..HEAD`, `repo-diff` 확인
- [ ] issue #1051과 PR assignee를 `debop`으로 설정
- [ ] issue milestone `1.12.0`과 labels를 PR에 복사
- [ ] PR body 마지막 Markdown `##` section을 `## DoD Status`로 작성
- [ ] exact local HEAD와 live PR head OID가 일치하는지 확인하고 metadata/body를 직접 검증한다.

```bash
HEAD_OID="$(git rev-parse HEAD)"
PR_NUMBER="$(gh pr view --json number --jq '.number')"
PR_OID="$(gh pr view "$PR_NUMBER" --json headRefOid --jq '.headRefOid')"
test "$HEAD_OID" = "$PR_OID"
gh issue view 1051 --json assignees,milestone,labels,state
gh pr view "$PR_NUMBER" \
  --json body,assignees,milestone,labels,headRefOid,reviewDecision
test "$(gh pr view "$PR_NUMBER" --json body --jq '.body' | \
  rg '^## ' | tail -n 1)" = "## DoD Status"
```

- [ ] required checks와 unresolved review thread를 exact head에서 조회한다.

```bash
gh pr checks "$PR_NUMBER" --required
test "$(gh pr view "$PR_NUMBER" --json reviewDecision --jq '.reviewDecision')" != "CHANGES_REQUESTED"
test "$(
  gh api graphql \
    -F owner=bluetape4k \
    -F name=bluetape4k-projects \
    -F number="$PR_NUMBER" \
    -f query='
      query($owner: String!, $name: String!, $number: Int!) {
        repository(owner: $owner, name: $name) {
          pullRequest(number: $number) {
            reviewThreads(first: 100) {
              nodes { isResolved }
            }
          }
        }
      }' \
    --jq '[.data.repository.pullRequest.reviewThreads.nodes[] | select(.isResolved == false)] | length'
)" = "0"
test "$(gh pr view "$PR_NUMBER" --json headRefOid --jq '.headRefOid')" = "$HEAD_OID"
```

- [ ] merge-ready 보고 직전에 위 OID/check/review-thread 명령을 다시 실행해 stale green을 배제한다.
- [ ] merge는 별도 사용자 승인 전 실행하지 않음

## Step 3-R Plan Review

| Perspective | Initial blockers | Incorporated repairs | Final |
|---|---:|---|---:|
| Performance | P1 2, P2 2 | Reactor own-rail `publishOn`, bounded provider shutdown, repeated stress 제거 | P0 0, P1 0 |
| Stability | P1 3, P2 3 | terminal signal 보존, failure-aware barrier, alias-keyed event ledger, bounded cleanup/XML gate | P0 0, P1 0 |
| Security | P1 2, P2 3 | interruption-safe teardown, privacy KDoc, uniform redaction, global-state/output grep | P0 0, P1 0 |
| Operations | P1 4, P2 1 | false-green 반복 제거, structural ordering evidence, XML duration, exact-head delivery gates | P0 0, P1 0 |
| Developer/API | P1 6, P2 2 | private captured-result contracts, Java `Duration` helpers, exact Spring/Ktor APIs와 `ApplicationTestBuilder` receiver | P0 0, P1 0 |
| User/caller | P1 4, P2 2 | complete bilingual examples, public KDoc ledger, safe diagnostics, parity/compatibility gates | P0 0, P1 0 |

- [x] Six independent review perspectives converged with P0=0 and P1=0.
- [x] Open user questions: none.
- [x] P2 findings were either incorporated into executable gates or retained as implementation review checks; P2 does not block the Type A implementation gate.
- [x] Existing test classpath was verified without build changes: Reactor Core 3.8.6, kotlinx-coroutines-reactor 1.11.0, OpenTelemetry SDK 1.63.0, and the repository JUnit 5 project dependency are already available.
- [x] Plan integrity checks: `git diff --check`, even code-fence count, exact Task 1–8 structure, required public-surface names, and stale-pattern negative grep.
- [x] The final plan SHA-256 is computed after this document stops changing and recorded in the external approval handoff rather than embedded in this self-hashing document.

## 위험과 완화

| 위험 | 완화 | stop/reopen 조건 |
|---|---|---|
| fixture가 framework type에 결합 | public API import audit | OTel/Reactor/Spring/Ktor type이 main fixture에 들어가면 재설계 |
| cancellation을 failure로 오분류 | terminal별 negative self-test | production adapter 변경 없이는 구분 불가하면 spec reopen |
| cancellation 후 worker probe race | `finallyCompleted` handshake | handshake 없이만 검증 가능하면 중단 |
| Reactor/Ktor global state 오염 | local scheduler/SDK와 no-global grep | global setter/hook 필요 시 중단 |
| concurrent test hang | failure-aware barrier, 5초/15초 guard | bounded teardown 증명 실패 시 해당 adapter 미완료 |
| raw marker 노출 | CR/LF canary와 관계형 message | redaction 없이 디버깅 불가하다는 요구가 생기면 spec reopen |
| dependency 부족 | 기존 test classpath에서 compile 확인 | build script 변경 필요 시 spec reopen |

## 롤백

- shared fixture API가 adapter evidence를 표현하지 못하면 소비 adapter 구현을 되돌리고 Task 1의 provider-neutral fixture commit만 유지한 채 spec을 다시 연다.
- 특정 framework adapter가 기존 public API 수정 없이는 계약을 만족하지 못하면 해당 adapter commit만 되돌리고 production defect를 별도 issue로 분리한다.
- flaky/hang이 발생하면 timeout을 늘리지 않는다. gate와 lifecycle ownership을 수정하고 결정성을 증명할 수 없으면 해당 scenario를 미완료로 남긴다.
- PR 전 rollback은 task별 commit revert로 수행한다. push/PR 이후 merge는 별도 승인 없이는 하지 않는다.

## 계획 완료 기준

- 모든 acceptance criterion이 Task 1–8에 매핑됨
- public API와 exact files가 승인된 spec과 일치
- 각 task에 RED/GREEN 명령과 예상 결과가 있음
- production/build/dependency 변경이 없음
- cancellation/deadline/resource lifecycle이 별도로 검증됨
- 계획 6개 관점 검토에서 P0/P1=0
- 사용자가 계획을 승인한 뒤에만 Task 1 구현 시작
