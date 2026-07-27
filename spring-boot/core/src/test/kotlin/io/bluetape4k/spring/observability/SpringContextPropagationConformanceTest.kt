package io.bluetape4k.spring.observability

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.junit5.coroutines.DEFAULT_CANCELLATION_CONTRACT_TIMEOUT
import io.bluetape4k.junit5.observability.ContextCleanupExpectation
import io.bluetape4k.junit5.observability.ContextCleanupProbe
import io.bluetape4k.junit5.observability.ContextIsolationExpectation
import io.bluetape4k.junit5.observability.ContextIsolationObservation
import io.bluetape4k.junit5.observability.ContextIsolationSample
import io.bluetape4k.junit5.observability.ContextIsolationSampleExpectation
import io.bluetape4k.junit5.observability.ContextMarkerExpectation
import io.bluetape4k.junit5.observability.ContextMarkerExpectationMode
import io.bluetape4k.junit5.observability.ContextMarkerObservation
import io.bluetape4k.junit5.observability.ContextObservationPoint
import io.bluetape4k.junit5.observability.ContextProbeLocation
import io.bluetape4k.junit5.observability.ContextPropagationBoundary
import io.bluetape4k.junit5.observability.ContextPropagationExpectation
import io.bluetape4k.junit5.observability.ContextPropagationObservation
import io.bluetape4k.junit5.observability.ContextPropagationScenario
import io.bluetape4k.junit5.observability.ContextPropagationTerminal
import io.bluetape4k.junit5.observability.ContextRequestAlias
import io.bluetape4k.junit5.observability.assertContextIsolation
import io.bluetape4k.junit5.observability.assertContextPropagationConformance
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.tck.TestObservationRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

@Timeout(
    value = 15,
    unit = TimeUnit.SECONDS,
    threadMode = Timeout.ThreadMode.SAME_THREAD,
)
class SpringContextPropagationConformanceTest {

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
    fun `spring observation failure propagates and cleans registry`() = runTest {
        val captured = runSpringScenario(ContextPropagationScenario.FAILURE)
        check(captured.thrown?.javaClass == IllegalStateException::class.java)
        assertContextPropagationConformance(
            captured.observation,
            springExpectation(
                ContextPropagationScenario.FAILURE,
                ContextPropagationTerminal.FAILURE,
            ),
        )
    }

    @Test
    fun `spring child cancellation propagates and cleans registry`() = runTest {
        val captured = runSpringScenario(ContextPropagationScenario.CANCELLATION)
        check(captured.thrown is CancellationException)
        check(captured.thrown !is TimeoutCancellationException)
        assertContextPropagationConformance(
            captured.observation,
            springExpectation(
                ContextPropagationScenario.CANCELLATION,
                ContextPropagationTerminal.CANCELLATION,
            ),
        )
    }

    @Test
    fun `spring observation deadline propagates and cleans registry`() = runTest {
        val captured = runSpringScenario(ContextPropagationScenario.DEADLINE)
        check(captured.thrown?.javaClass == TimeoutCancellationException::class.java)
        assertContextPropagationConformance(
            captured.observation,
            springExpectation(
                ContextPropagationScenario.DEADLINE,
                ContextPropagationTerminal.DEADLINE_EXCEEDED,
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

    @Test
    fun `spring isolation preserves participant failure after release`() = runTest {
        val failure = SyntheticSpringIsolationFailure(Any())

        val thrown = assertFailsWith<SyntheticSpringIsolationFailure> {
            runSpringIsolationScenarioWithFailure(failure)
        }

        check(thrown === failure)
    }
}

private val hangGuard = DEFAULT_CANCELLATION_CONTRACT_TIMEOUT
private val semanticDeadline = 250.milliseconds.also {
    check(it < hangGuard)
}
private const val springMarkerA = "synthetic-spring-parent-a"
private const val springMarkerB = "synthetic-spring-parent-b"

private class CapturedScenario(
    val observation: ContextPropagationObservation,
    val thrown: Throwable?,
)

private class SyntheticSpringIsolationFailure(
    @Suppress("unused")
    private val identityToken: Any,
): RuntimeException("synthetic post-release failure")

private enum class SpringConformanceEvent {
    READY,
    RELEASED,
    TERMINAL_OBSERVED,
    FINALLY_COMPLETED,
    CLEANUP_PROBED,
}

private class SpringConformanceEventEntry(
    val requestAlias: ContextRequestAlias,
    val event: SpringConformanceEvent,
    val sequence: Long,
)

private class SpringConformanceEventLedger {
    private val sequence = AtomicLong()
    private val events = ConcurrentLinkedQueue<SpringConformanceEventEntry>()

    fun record(
        requestAlias: ContextRequestAlias,
        event: SpringConformanceEvent,
    ) {
        events += SpringConformanceEventEntry(
            requestAlias,
            event,
            sequence.incrementAndGet(),
        )
    }

    fun assertSingleScenarioOrder() {
        val entries = events.filter { it.requestAlias == ContextRequestAlias.SINGLE }
        val counts = entries.groupingBy(SpringConformanceEventEntry::event).eachCount()
        listOf(
            SpringConformanceEvent.TERMINAL_OBSERVED,
            SpringConformanceEvent.FINALLY_COMPLETED,
            SpringConformanceEvent.CLEANUP_PROBED,
        ).forEach { event ->
            check(counts[event] == 1) {
                "Spring lifecycle event count mismatch: alias=SINGLE, event=$event"
            }
        }
        check(counts[SpringConformanceEvent.READY] == null)
        check(counts[SpringConformanceEvent.RELEASED] == null)
        assertCleanupAfterFinally(ContextRequestAlias.SINGLE, entries)
    }

    fun assertIsolationOrder() {
        val snapshot = events.toList()
        val participantEntries = listOf(
            ContextRequestAlias.REQUEST_A,
            ContextRequestAlias.REQUEST_B,
        ).associateWith { alias ->
            snapshot.filter { it.requestAlias == alias }.also { entries ->
                val counts = entries.groupingBy(SpringConformanceEventEntry::event).eachCount()
                SpringConformanceEvent.entries.forEach { event ->
                    check(counts[event] == 1) {
                        "Spring lifecycle event count mismatch: alias=$alias, event=$event"
                    }
                }
            }
        }
        val lastReady = participantEntries.values
            .flatten()
            .filter { it.event == SpringConformanceEvent.READY }
            .maxOf(SpringConformanceEventEntry::sequence)
        val firstRelease = participantEntries.values
            .flatten()
            .filter { it.event == SpringConformanceEvent.RELEASED }
            .minOf(SpringConformanceEventEntry::sequence)
        check(lastReady < firstRelease)
        participantEntries.forEach { (alias, entries) ->
            assertCleanupAfterFinally(alias, entries)
        }
    }

    private fun assertCleanupAfterFinally(
        alias: ContextRequestAlias,
        entries: List<SpringConformanceEventEntry>,
    ) {
        val terminalSequence = entries.single {
            it.event == SpringConformanceEvent.TERMINAL_OBSERVED
        }.sequence
        val finallySequence = entries.single {
            it.event == SpringConformanceEvent.FINALLY_COMPLETED
        }.sequence
        val cleanupSequence = entries.single {
            it.event == SpringConformanceEvent.CLEANUP_PROBED
        }.sequence
        check(terminalSequence < cleanupSequence) {
            "Spring terminal cleanup order mismatch: alias=$alias"
        }
        check(finallySequence < cleanupSequence) {
            "Spring lifecycle cleanup order mismatch: alias=$alias"
        }
    }
}

private fun springExpectation(
    scenario: ContextPropagationScenario,
    terminal: ContextPropagationTerminal,
): ContextPropagationExpectation =
    ContextPropagationExpectation(
        boundary = ContextPropagationBoundary.SPRING_OBSERVATION,
        scenario = scenario,
        requestAlias = ContextRequestAlias.SINGLE,
        markerExpectations = listOf(
            ContextMarkerExpectation(ContextObservationPoint.BOUNDARY_ENTER, springMarkerA),
            ContextMarkerExpectation(ContextObservationPoint.AFTER_SUSPENSION, springMarkerA),
            ContextMarkerExpectation(ContextObservationPoint.BEFORE_TERMINAL, springMarkerA),
        ),
        cleanupExpectations = listOf(
            ContextCleanupExpectation(ContextProbeLocation.CALLER, null),
        ),
        expectedTerminal = terminal,
    )

private fun springIsolationExpectation(): ContextIsolationExpectation =
    ContextIsolationExpectation(
        boundary = ContextPropagationBoundary.SPRING_OBSERVATION,
        samples = listOf(
            ContextIsolationSampleExpectation(
                requestAlias = ContextRequestAlias.REQUEST_A,
                mode = ContextMarkerExpectationMode.EXACT,
                expectedMarker = springMarkerA,
                minimumObservationCount = 3,
            ),
            ContextIsolationSampleExpectation(
                requestAlias = ContextRequestAlias.REQUEST_B,
                mode = ContextMarkerExpectationMode.EXACT,
                expectedMarker = springMarkerB,
                minimumObservationCount = 3,
            ),
        ),
        cleanupExpectations = listOf(
            ContextCleanupExpectation(ContextProbeLocation.CALLER, null),
        ),
    )

private suspend fun runSpringScenario(
    scenario: ContextPropagationScenario,
): CapturedScenario =
    supervisorScope {
        check(scenario != ContextPropagationScenario.ISOLATION) {
            "Isolation uses runSpringIsolationScenario"
        }

        val registry = TestObservationRegistry.create()
        val observations = mutableListOf<ContextMarkerObservation>()
        val started = CompletableDeferred<Unit>()
        val finallyCompleted = CompletableDeferred<Unit>()
        val ledger = SpringConformanceEventLedger()
        val children = mutableListOf<Deferred<*>>()

        withSpringChildrenCleanup(
            children = children,
            beforeCleanup = {
                started.complete(Unit)
            },
        ) {
            val child = async {
                try {
                    registry.observeMarkers(springMarkerA, observations) {
                        started.complete(Unit)
                        when (scenario) {
                            ContextPropagationScenario.SUCCESS -> Unit
                            ContextPropagationScenario.FAILURE ->
                                error("synthetic Spring observation failure")

                            ContextPropagationScenario.CANCELLATION ->
                                awaitCancellation()

                            ContextPropagationScenario.DEADLINE ->
                                withTimeout(semanticDeadline) {
                                    awaitCancellation()
                                }

                            else -> error("Isolation uses runSpringIsolationScenario")
                        }
                    }
                } finally {
                    finallyCompleted.complete(Unit)
                }
            }
            children += child
            started.awaitGateWithin()
            if (scenario == ContextPropagationScenario.CANCELLATION) {
                child.cancel(CancellationException("synthetic Spring child cancellation"))
            }
            val thrown = child.captureTerminalWithin()
            ledger.record(
                ContextRequestAlias.SINGLE,
                SpringConformanceEvent.TERMINAL_OBSERVED,
            )
            finallyCompleted.awaitGateWithin()
            ledger.record(
                ContextRequestAlias.SINGLE,
                SpringConformanceEvent.FINALLY_COMPLETED,
            )
            val callerMarker = registry.currentMarker()
            ledger.record(
                ContextRequestAlias.SINGLE,
                SpringConformanceEvent.CLEANUP_PROBED,
            )
            ledger.assertSingleScenarioOrder()

            CapturedScenario(
                observation = ContextPropagationObservation(
                    boundary = ContextPropagationBoundary.SPRING_OBSERVATION,
                    scenario = scenario,
                    requestAlias = ContextRequestAlias.SINGLE,
                    markerObservations = observations.toList(),
                    cleanupProbes = listOf(
                        ContextCleanupProbe(ContextProbeLocation.CALLER, callerMarker),
                    ),
                    terminal = terminalFor(scenario),
                ),
                thrown = thrown,
            )
        }
    }

private suspend fun runSpringIsolationScenario(): ContextIsolationObservation =
    runSpringIsolationScenarioWithFailure()

private suspend fun runSpringIsolationScenarioWithFailure(
    failureAfterRelease: Throwable? = null,
): ContextIsolationObservation =
    supervisorScope {
        val registryA = TestObservationRegistry.create()
        val registryB = TestObservationRegistry.create()
        val observationsA = ConcurrentLinkedQueue<String?>()
        val observationsB = ConcurrentLinkedQueue<String?>()
        val readyA = CompletableDeferred<Unit>()
        val readyB = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finallyA = CompletableDeferred<Unit>()
        val finallyB = CompletableDeferred<Unit>()
        val firstFailure = AtomicReference<Throwable?>()
        val ledger = SpringConformanceEventLedger()
        val children = mutableListOf<Deferred<*>>()

        withSpringChildrenCleanup(
            children = children,
            beforeCleanup = {
                readyA.complete(Unit)
                readyB.complete(Unit)
                release.complete(Unit)
            },
        ) {
            val childA = async {
                runSpringIsolationParticipant(
                    registry = registryA,
                    marker = springMarkerA,
                    alias = ContextRequestAlias.REQUEST_A,
                    observations = observationsA,
                    ownReady = readyA,
                    readyA = readyA,
                    readyB = readyB,
                    release = release,
                    finallyCompleted = finallyA,
                    firstFailure = firstFailure,
                    ledger = ledger,
                    failureAfterRelease = failureAfterRelease,
                )
            }
            val childB = async {
                runSpringIsolationParticipant(
                    registry = registryB,
                    marker = springMarkerB,
                    alias = ContextRequestAlias.REQUEST_B,
                    observations = observationsB,
                    ownReady = readyB,
                    readyA = readyA,
                    readyB = readyB,
                    release = release,
                    finallyCompleted = finallyB,
                    firstFailure = firstFailure,
                    ledger = ledger,
                    failureAfterRelease = null,
                )
            }
            children += childA
            children += childB

            readyA.awaitGateWithin()
            readyB.awaitGateWithin()
            firstFailure.get()?.let { throw it }
            release.complete(Unit)

            val thrownA = childA.captureTerminalWithin()
            ledger.record(
                ContextRequestAlias.REQUEST_A,
                SpringConformanceEvent.TERMINAL_OBSERVED,
            )
            val thrownB = childB.captureTerminalWithin()
            ledger.record(
                ContextRequestAlias.REQUEST_B,
                SpringConformanceEvent.TERMINAL_OBSERVED,
            )
            (firstFailure.get() ?: thrownA ?: thrownB)?.let { throw it }

            finallyA.awaitGateWithin()
            ledger.record(
                ContextRequestAlias.REQUEST_A,
                SpringConformanceEvent.FINALLY_COMPLETED,
            )
            finallyB.awaitGateWithin()
            ledger.record(
                ContextRequestAlias.REQUEST_B,
                SpringConformanceEvent.FINALLY_COMPLETED,
            )

            val callerMarkerA = registryA.currentMarker()
            ledger.record(
                ContextRequestAlias.REQUEST_A,
                SpringConformanceEvent.CLEANUP_PROBED,
            )
            val callerMarkerB = registryB.currentMarker()
            ledger.record(
                ContextRequestAlias.REQUEST_B,
                SpringConformanceEvent.CLEANUP_PROBED,
            )
            ledger.assertIsolationOrder()

            ContextIsolationObservation(
                boundary = ContextPropagationBoundary.SPRING_OBSERVATION,
                samples = listOf(
                    ContextIsolationSample(
                        ContextRequestAlias.REQUEST_A,
                        observationsA.toList(),
                    ),
                    ContextIsolationSample(
                        ContextRequestAlias.REQUEST_B,
                        observationsB.toList(),
                    ),
                ),
                cleanupProbes = listOf(
                    ContextCleanupProbe(
                        ContextProbeLocation.CALLER,
                        callerMarkerA ?: callerMarkerB,
                    ),
                ),
            )
        }
    }

private suspend fun runSpringIsolationParticipant(
    registry: ObservationRegistry,
    marker: String,
    alias: ContextRequestAlias,
    observations: ConcurrentLinkedQueue<String?>,
    ownReady: CompletableDeferred<Unit>,
    readyA: CompletableDeferred<Unit>,
    readyB: CompletableDeferred<Unit>,
    release: CompletableDeferred<Unit>,
    finallyCompleted: CompletableDeferred<Unit>,
    firstFailure: AtomicReference<Throwable?>,
    ledger: SpringConformanceEventLedger,
    failureAfterRelease: Throwable?,
) {
    try {
        registry.observeSpringSuspending(marker) {
            observations += registry.currentMarker()
            ledger.record(alias, SpringConformanceEvent.READY)
            ownReady.complete(Unit)
            readyA.awaitGateWithin()
            readyB.awaitGateWithin()
            release.awaitGateWithin()
            ledger.record(alias, SpringConformanceEvent.RELEASED)
            failureAfterRelease?.let { throw it }
            yield()
            observations += registry.currentMarker()
            observations += registry.currentMarker()
        }
    } catch (e: Exception) {
        recordSpringIsolationFailure(
            e,
            firstFailure,
            readyA,
            readyB,
            release,
        )
        throw e
    } catch (e: Error) {
        recordSpringIsolationFailure(
            e,
            firstFailure,
            readyA,
            readyB,
            release,
        )
        throw e
    } finally {
        finallyCompleted.complete(Unit)
    }
}

private fun recordSpringIsolationFailure(
    failure: Throwable,
    firstFailure: AtomicReference<Throwable?>,
    readyA: CompletableDeferred<Unit>,
    readyB: CompletableDeferred<Unit>,
    release: CompletableDeferred<Unit>,
) {
    firstFailure.compareAndSet(null, failure)
    readyA.complete(Unit)
    readyB.complete(Unit)
    release.complete(Unit)
}

private suspend fun ObservationRegistry.observeMarkers(
    marker: String,
    observations: MutableList<ContextMarkerObservation>,
    terminalAction: suspend () -> Unit,
) {
    observeSpringSuspending(name = marker) {
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

private fun ObservationRegistry.currentMarker(): String? =
    currentObservation?.context?.name

private fun terminalFor(
    scenario: ContextPropagationScenario,
): ContextPropagationTerminal =
    when (scenario) {
        ContextPropagationScenario.SUCCESS -> ContextPropagationTerminal.SUCCESS
        ContextPropagationScenario.FAILURE -> ContextPropagationTerminal.FAILURE
        ContextPropagationScenario.CANCELLATION -> ContextPropagationTerminal.CANCELLATION
        ContextPropagationScenario.DEADLINE -> ContextPropagationTerminal.DEADLINE_EXCEEDED
        else -> error("Isolation does not have a single terminal")
    }

private suspend fun CompletableDeferred<Unit>.awaitGateWithin() {
    try {
        withTimeout(hangGuard) {
            await()
        }
    } catch (e: TimeoutCancellationException) {
        throw AssertionError("Timed out waiting for Spring conformance gate", e)
    }
}

private suspend fun Deferred<Unit>.captureTerminalWithin(): Throwable? =
    try {
        withTimeout(hangGuard) {
            await()
        }
        null
    } catch (e: TimeoutCancellationException) {
        if (isCompleted) {
            e
        } else {
            throw AssertionError("Timed out waiting for Spring terminal", e)
        }
    } catch (e: CancellationException) {
        e
    } catch (e: Exception) {
        e
    }

private suspend fun <T> withSpringChildrenCleanup(
    children: MutableList<Deferred<*>>,
    beforeCleanup: () -> Unit,
    block: suspend () -> T,
): T {
    var primaryFailure: Throwable? = null
    try {
        return block()
    } catch (e: Exception) {
        primaryFailure = e
        throw e
    } catch (e: Error) {
        primaryFailure = e
        throw e
    } finally {
        cleanupSpringChildren(
            primaryFailure,
            beforeCleanup,
            children,
        )
    }
}

private suspend fun cleanupSpringChildren(
    primaryFailure: Throwable?,
    beforeCleanup: () -> Unit,
    children: List<Deferred<*>>,
) {
    var aggregatedFailure = primaryFailure
    withContext(NonCancellable) {
        val actions = buildList<suspend () -> Unit> {
            add {
                beforeCleanup()
            }
            children.forEach { child ->
                add {
                    child.cancel(CancellationException("Spring conformance cleanup"))
                }
            }
            add {
                withTimeout(hangGuard) {
                    children.joinAll()
                }
            }
        }
        actions.forEach { action ->
            val cleanupFailure = try {
                action()
                null
            } catch (e: TimeoutCancellationException) {
                AssertionError("Timed out cleaning Spring conformance children", e)
            } catch (e: Exception) {
                e
            } catch (e: Error) {
                e
            }
            cleanupFailure?.let { failure ->
                val aggregate = aggregatedFailure
                if (aggregate == null) {
                    aggregatedFailure = failure
                } else if (aggregate !== failure && aggregate.suppressed.none { it === failure }) {
                    aggregate.addSuppressed(failure)
                }
            }
        }
    }
    if (primaryFailure == null) {
        aggregatedFailure?.let { throw it }
    }
}
