package io.bluetape4k.opentelemetry.context

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
import io.bluetape4k.opentelemetry.coroutines.withOtelContext
import io.opentelemetry.context.Context
import io.opentelemetry.context.ContextKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

internal val propagationMarkerKey: ContextKey<String> =
    ContextKey.named("bluetape4k-context-propagation-test-marker")

internal const val parentMarkerA = "parent-A"
internal const val parentMarkerB = "parent-B"

private const val reactorOtelContextKey = "bluetape4k.otel.context"
private const val probeMarker = "probe-marker"

private val hangGuard = DEFAULT_CANCELLATION_CONTRACT_TIMEOUT
private val semanticDeadline = 250.milliseconds.also {
    check(it < hangGuard)
}

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

internal fun <T> captureExecutorTerminal(
    future: Future<T>,
    timeout: Duration,
): Throwable? =
    try {
        future.getWithin(timeout)
        null
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        throw e
    } catch (e: ExecutionException) {
        val cause = e.cause ?: e
        if (cause is Error) {
            throw cause
        }
        cause
    } catch (e: TimeoutException) {
        throw AssertionError("Timed out waiting for test executor result", e)
    } catch (e: CancellationException) {
        e
    }

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

internal fun <T> withExecutorCleanup(
    executor: ExecutorService,
    cancel: ExecutorService.() -> Unit = {},
    shutdown: ExecutorService.() -> Unit = { shutdownAndAssertTermination() },
    block: () -> T,
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
        val restoreInterrupt = Thread.interrupted()
        var aggregatedFailure = primaryFailure
        listOf(cancel, shutdown).forEach { cleanup ->
            var cleanupFailure: Throwable? = null
            try {
                executor.cleanup()
            } catch (e: Exception) {
                cleanupFailure = e
            } catch (e: Error) {
                cleanupFailure = e
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

        try {
            if (primaryFailure == null) {
                aggregatedFailure?.let { throw it }
            }
        } finally {
            if (restoreInterrupt) {
                Thread.currentThread().interrupt()
            }
        }
    }
}

internal class CapturedScenario(
    val observation: ContextPropagationObservation,
    val thrown: Throwable?,
)

internal inline fun <reified T: Throwable> CapturedScenario.assertThrownExactly() {
    check(thrown?.javaClass == T::class.java) {
        "Scenario terminal type mismatch; values redacted"
    }
}

private class CoroutineTerminalResult(
    val thrown: Throwable?,
    val workerMarker: String?,
)

private class CoroutineIsolationResult(
    val probeMarker: String?,
    val workerMarker: String?,
)

internal suspend fun runCoroutineScenario(
    scenario: ContextPropagationScenario,
): CapturedScenario {
    check(scenario != ContextPropagationScenario.ISOLATION) {
        "Isolation uses runCoroutineIsolationScenario"
    }

    val executor = Executors.newSingleThreadExecutor()
    val dispatcher = executor.asCoroutineDispatcher()
    val ledger = ConformanceEventLedger()
    val observations = mutableListOf<ContextMarkerObservation>()
    return withCoroutineResources(dispatcher, executor) {
        val result = withContext(dispatcher) {
            supervisorScope {
                val started = CompletableDeferred<Unit>()
                val cancellationSignal = CancellationException("synthetic coroutine cancellation")
                val child = async {
                    executeCoroutineTerminal(
                        dispatcher,
                        scenario,
                        observations,
                        started,
                        ledger,
                    )
                }
                withCoroutineChildCleanup(
                    children = listOf(child),
                    beforeCleanup = {
                        started.complete(Unit)
                    },
                ) {
                    started.awaitGateWithin()
                    if (scenario == ContextPropagationScenario.CANCELLATION) {
                        child.cancel(cancellationSignal)
                    }
                    val thrown = child.captureTerminalWithin()
                    ledger.record(ContextRequestAlias.SINGLE, ConformanceEvent.TERMINAL_OBSERVED)
                    val workerMarker = currentMarker()
                    ledger.record(ContextRequestAlias.SINGLE, ConformanceEvent.CLEANUP_PROBED)
                    ledger.assertSingleScenarioOrder()
                    CoroutineTerminalResult(thrown, workerMarker)
                }
            }
        }
        capturedScenario(
            ContextPropagationBoundary.COROUTINE,
            scenario,
            observations,
            result.workerMarker,
            terminalFor(scenario),
            result.thrown,
        )
    }
}

internal suspend fun runCoroutineIsolationScenario(): ContextIsolationObservation {
    val executor = Executors.newSingleThreadExecutor()
    val dispatcher = executor.asCoroutineDispatcher()
    val ledger = ConformanceEventLedger()
    val markersA = mutableListOf<String?>()
    val markersB = mutableListOf<String?>()

    return withCoroutineResources(dispatcher, executor) {
        val result = withContext(dispatcher) {
            supervisorScope {
                val readyA = CompletableDeferred<Unit>()
                val readyB = CompletableDeferred<Unit>()
                val childA = async {
                    executeCoroutineIsolationParticipant(
                        dispatcher,
                        ContextRequestAlias.REQUEST_A,
                        parentMarkerA,
                        markersA,
                        readyA,
                        readyB,
                        ledger,
                    )
                }
                val childB = async {
                    executeCoroutineIsolationParticipant(
                        dispatcher,
                        ContextRequestAlias.REQUEST_B,
                        parentMarkerB,
                        markersB,
                        readyB,
                        readyA,
                        ledger,
                    )
                }
                withCoroutineChildCleanup(
                    children = listOf(childA, childB),
                    beforeCleanup = {
                        readyA.complete(Unit)
                        readyB.complete(Unit)
                    },
                ) {
                    childA.awaitCompletedWithin()
                    ledger.record(ContextRequestAlias.REQUEST_A, ConformanceEvent.TERMINAL_OBSERVED)
                    childB.awaitCompletedWithin()
                    ledger.record(ContextRequestAlias.REQUEST_B, ConformanceEvent.TERMINAL_OBSERVED)

                    val probe = withOtelContext(
                        coroutineContext = dispatcher,
                        otelContext = otelContext(probeMarker),
                    ) {
                        currentMarker()
                    }
                    val workerProbeA = currentMarker()
                    ledger.record(ContextRequestAlias.REQUEST_A, ConformanceEvent.CLEANUP_PROBED)
                    val workerProbeB = currentMarker()
                    ledger.record(ContextRequestAlias.REQUEST_B, ConformanceEvent.CLEANUP_PROBED)
                    check(workerProbeA == null && workerProbeB == null) {
                        "Coroutine worker context was not restored"
                    }
                    ledger.assertIsolationOrder()
                    CoroutineIsolationResult(probe, workerProbeA)
                }
            }
        }
        ContextIsolationObservation(
            boundary = ContextPropagationBoundary.COROUTINE,
            samples = listOf(
                ContextIsolationSample(ContextRequestAlias.REQUEST_A, markersA.toList()),
                ContextIsolationSample(ContextRequestAlias.REQUEST_B, markersB.toList()),
                ContextIsolationSample(ContextRequestAlias.PROBE, listOf(result.probeMarker)),
            ),
            cleanupProbes = listOf(
                ContextCleanupProbe(ContextProbeLocation.CALLER, currentMarker()),
                ContextCleanupProbe(ContextProbeLocation.WORKER, result.workerMarker),
            ),
        )
    }
}

internal fun coroutineIsolationExpectation(): ContextIsolationExpectation =
    ContextIsolationExpectation(
        boundary = ContextPropagationBoundary.COROUTINE,
        samples = listOf(
            ContextIsolationSampleExpectation(
                requestAlias = ContextRequestAlias.REQUEST_A,
                mode = ContextMarkerExpectationMode.EXACT,
                expectedMarker = parentMarkerA,
                minimumObservationCount = 3,
            ),
            ContextIsolationSampleExpectation(
                requestAlias = ContextRequestAlias.REQUEST_B,
                mode = ContextMarkerExpectationMode.EXACT,
                expectedMarker = parentMarkerB,
                minimumObservationCount = 3,
            ),
            ContextIsolationSampleExpectation(
                requestAlias = ContextRequestAlias.PROBE,
                mode = ContextMarkerExpectationMode.NOT_IN,
                forbiddenMarkers = listOf(parentMarkerA, parentMarkerB),
            ),
        ),
        cleanupExpectations = listOf(
            ContextCleanupExpectation(ContextProbeLocation.CALLER, null),
            ContextCleanupExpectation(ContextProbeLocation.WORKER, null),
        ),
    )

internal fun runReactorScenario(
    scenario: ContextPropagationScenario,
): CapturedScenario {
    check(scenario == ContextPropagationScenario.SUCCESS) {
        "Task 2 supports only the success Reactor scenario"
    }

    val scheduler = Schedulers.newSingle("otel-context-conformance")
    val ledger = ConformanceEventLedger()
    val observations = mutableListOf<ContextMarkerObservation>()
    var thrown: Throwable? = null
    try {
        try {
            executeReactorSuccess(scheduler, observations, ledger)
                .block(DEFAULT_CANCELLATION_CONTRACT_TIMEOUT.toJavaDuration())
        } catch (e: Exception) {
            thrown = e
        }
        ledger.record(ContextRequestAlias.SINGLE, ConformanceEvent.TERMINAL_OBSERVED)
        val workerMarker = Mono.fromCallable(::currentMarker)
            .subscribeOn(scheduler)
            .block(DEFAULT_CANCELLATION_CONTRACT_TIMEOUT.toJavaDuration())
        ledger.record(ContextRequestAlias.SINGLE, ConformanceEvent.CLEANUP_PROBED)
        ledger.assertSingleScenarioOrder()
        return capturedSuccess(
            ContextPropagationBoundary.REACTOR,
            scenario,
            observations,
            workerMarker,
            thrown,
        )
    } finally {
        shutdownScheduler(scheduler)
    }
}

internal fun runExecutorScenario(
    scenario: ContextPropagationScenario,
): CapturedScenario {
    check(scenario == ContextPropagationScenario.SUCCESS) {
        "Task 2 supports only the success executor scenario"
    }

    val executor = Executors.newSingleThreadExecutor()
    val ledger = ConformanceEventLedger()
    val observations = mutableListOf<ContextMarkerObservation>()
    var future: Future<Unit>? = null
    return withExecutorCleanup(
        executor = executor,
        cancel = {
            future?.cancel(true)
        },
    ) {
        future = executeExecutorSuccess(executor, observations, ledger)
        val thrown = captureExecutorTerminal(future, DEFAULT_CANCELLATION_CONTRACT_TIMEOUT)
        ledger.record(ContextRequestAlias.SINGLE, ConformanceEvent.TERMINAL_OBSERVED)
        val workerMarker = executor.submit(Callable(::currentMarker))
            .getWithin(DEFAULT_CANCELLATION_CONTRACT_TIMEOUT)
        ledger.record(ContextRequestAlias.SINGLE, ConformanceEvent.CLEANUP_PROBED)
        ledger.assertSingleScenarioOrder()
        capturedSuccess(
            ContextPropagationBoundary.TASK_EXECUTOR,
            scenario,
            observations,
            workerMarker,
            thrown,
        )
    }
}

private suspend fun executeCoroutineTerminal(
    dispatcher: CoroutineDispatcher,
    scenario: ContextPropagationScenario,
    observations: MutableList<ContextMarkerObservation>,
    started: CompletableDeferred<Unit>,
    ledger: ConformanceEventLedger,
) {
    try {
        withOtelContext(
            coroutineContext = dispatcher,
            otelContext = otelContext(parentMarkerA),
        ) {
            observeCoroutineBody(observations)
            started.complete(Unit)
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
    } finally {
        started.complete(Unit)
        ledger.record(ContextRequestAlias.SINGLE, ConformanceEvent.FINALLY_COMPLETED)
    }
}

private suspend fun executeCoroutineIsolationParticipant(
    dispatcher: CoroutineDispatcher,
    alias: ContextRequestAlias,
    marker: String,
    observations: MutableList<String?>,
    ownReady: CompletableDeferred<Unit>,
    peerReady: CompletableDeferred<Unit>,
    ledger: ConformanceEventLedger,
) {
    try {
        withOtelContext(
            coroutineContext = dispatcher,
            otelContext = otelContext(marker),
        ) {
            observations += currentMarker()
            ledger.record(alias, ConformanceEvent.READY)
            ownReady.complete(Unit)
            peerReady.awaitGateWithin()
            ledger.record(alias, ConformanceEvent.RELEASED)
            yield()
            observations += currentMarker()
            observations += currentMarker()
        }
    } finally {
        peerReady.complete(Unit)
        ledger.record(alias, ConformanceEvent.FINALLY_COMPLETED)
    }
}

private suspend fun observeCoroutineBody(
    observations: MutableList<ContextMarkerObservation>,
) {
    observations += markerObservation(ContextObservationPoint.BOUNDARY_ENTER)
    yield()
    observations += markerObservation(ContextObservationPoint.AFTER_SUSPENSION)
    observations += markerObservation(ContextObservationPoint.BEFORE_TERMINAL)
}

private fun executeReactorSuccess(
    scheduler: Scheduler,
    observations: MutableList<ContextMarkerObservation>,
    ledger: ConformanceEventLedger,
): Mono<Unit> =
    Mono.deferContextual { view ->
        val captured = view.get<Context>(reactorOtelContextKey)
        Mono.fromCallable(
            captured.wrap(
                Callable {
                    observations += markerObservation(ContextObservationPoint.BOUNDARY_ENTER)
                    observations += markerObservation(ContextObservationPoint.AFTER_SUSPENSION)
                    observations += markerObservation(ContextObservationPoint.BEFORE_TERMINAL)
                    Unit
                },
            ),
        )
    }
        .contextWrite { it.put(reactorOtelContextKey, otelContext(parentMarkerA)) }
        .subscribeOn(scheduler)
        .doFinally {
            ledger.record(ContextRequestAlias.SINGLE, ConformanceEvent.FINALLY_COMPLETED)
        }

private fun executeExecutorSuccess(
    executor: ExecutorService,
    observations: MutableList<ContextMarkerObservation>,
    ledger: ConformanceEventLedger,
): Future<Unit> =
    executor.submit(
        otelContext(parentMarkerA).wrap(
            Callable {
                try {
                    observations += markerObservation(ContextObservationPoint.BOUNDARY_ENTER)
                    observations += markerObservation(ContextObservationPoint.AFTER_SUSPENSION)
                    observations += markerObservation(ContextObservationPoint.BEFORE_TERMINAL)
                    Unit
                } finally {
                    ledger.record(ContextRequestAlias.SINGLE, ConformanceEvent.FINALLY_COMPLETED)
                }
            },
        ),
    )

private fun markerObservation(point: ContextObservationPoint): ContextMarkerObservation =
    ContextMarkerObservation(point, currentMarker())

private suspend fun CompletableDeferred<Unit>.awaitGateWithin() {
    try {
        withTimeout(hangGuard) {
            await()
        }
    } catch (e: TimeoutCancellationException) {
        throw AssertionError("Timed out waiting for coroutine test gate", e)
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
            throw AssertionError("Timed out waiting for coroutine terminal", e)
        }
    } catch (e: CancellationException) {
        e
    } catch (e: Exception) {
        e
    }

private suspend fun Deferred<Unit>.awaitCompletedWithin() {
    try {
        withTimeout(hangGuard) {
            await()
        }
    } catch (e: TimeoutCancellationException) {
        if (isCompleted) {
            throw e
        }
        throw AssertionError("Timed out waiting for coroutine completion", e)
    }
}

private suspend fun cleanupCoroutineChildren(
    primaryFailure: Throwable?,
    beforeCleanup: () -> Unit,
    children: List<Deferred<*>>,
) {
    cleanupCoroutineActions(
        primaryFailure = primaryFailure,
        actions = buildList {
            add {
                beforeCleanup()
            }
            children.forEach { child ->
                add {
                    child.cancelAndJoin()
                }
            }
        },
    )
}

internal suspend fun <T> withCoroutineChildCleanup(
    children: List<Deferred<*>>,
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
        cleanupCoroutineChildren(primaryFailure, beforeCleanup, children)
    }
}

internal suspend fun cleanupCoroutineActions(
    primaryFailure: Throwable? = null,
    actions: List<suspend () -> Unit>,
) {
    var aggregatedFailure = primaryFailure
    withContext(NonCancellable) {
        actions.forEach { cleanup ->
            var cleanupFailure: Throwable? = null
            try {
                withTimeout(hangGuard) {
                    try {
                        cleanup()
                    } catch (e: Exception) {
                        cleanupFailure = e
                    } catch (e: Error) {
                        cleanupFailure = e
                    }
                }
            } catch (e: TimeoutCancellationException) {
                cleanupFailure = AssertionError("Timed out cancelling coroutine child", e)
            } catch (e: Exception) {
                cleanupFailure = e
            } catch (e: Error) {
                cleanupFailure = e
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
    aggregatedFailure?.let { throw it }
}

private suspend fun <T> withCoroutineResources(
    dispatcher: ExecutorCoroutineDispatcher,
    executor: ExecutorService,
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
        val restoreInterrupt = Thread.interrupted()
        var aggregatedFailure = primaryFailure
        listOf<() -> Unit>(
            dispatcher::close,
            executor::shutdownAndAssertTermination,
        ).forEach { cleanup ->
            var cleanupFailure: Throwable? = null
            try {
                cleanup()
            } catch (e: Exception) {
                cleanupFailure = e
            } catch (e: Error) {
                cleanupFailure = e
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

        try {
            if (primaryFailure == null) {
                aggregatedFailure?.let { throw it }
            }
        } finally {
            if (restoreInterrupt) {
                Thread.currentThread().interrupt()
            }
        }
    }
}

private fun terminalFor(scenario: ContextPropagationScenario): ContextPropagationTerminal =
    when (scenario) {
        ContextPropagationScenario.SUCCESS -> ContextPropagationTerminal.SUCCESS
        ContextPropagationScenario.FAILURE -> ContextPropagationTerminal.FAILURE
        ContextPropagationScenario.CANCELLATION -> ContextPropagationTerminal.CANCELLATION
        ContextPropagationScenario.DEADLINE -> ContextPropagationTerminal.DEADLINE_EXCEEDED
        ContextPropagationScenario.ISOLATION ->
            error("Isolation uses coroutineIsolationExpectation")
    }

private fun capturedScenario(
    boundary: ContextPropagationBoundary,
    scenario: ContextPropagationScenario,
    observations: List<ContextMarkerObservation>,
    workerMarker: String?,
    terminal: ContextPropagationTerminal,
    thrown: Throwable?,
): CapturedScenario =
    CapturedScenario(
        observation = ContextPropagationObservation(
            boundary = boundary,
            scenario = scenario,
            requestAlias = ContextRequestAlias.SINGLE,
            markerObservations = observations.toList(),
            cleanupProbes = listOf(
                ContextCleanupProbe(ContextProbeLocation.CALLER, currentMarker()),
                ContextCleanupProbe(ContextProbeLocation.WORKER, workerMarker),
            ),
            terminal = terminal,
        ),
        thrown = thrown,
    )

private fun capturedSuccess(
    boundary: ContextPropagationBoundary,
    scenario: ContextPropagationScenario,
    observations: List<ContextMarkerObservation>,
    workerMarker: String?,
    thrown: Throwable?,
): CapturedScenario =
    capturedScenario(
        boundary,
        scenario,
        observations,
        workerMarker,
        ContextPropagationTerminal.SUCCESS,
        thrown,
    )

private fun shutdownScheduler(scheduler: Scheduler) {
    scheduler.disposeGracefully()
        .block(DEFAULT_CANCELLATION_CONTRACT_TIMEOUT.toJavaDuration())
    check(scheduler.isDisposed) { "Test scheduler did not terminate" }
}
