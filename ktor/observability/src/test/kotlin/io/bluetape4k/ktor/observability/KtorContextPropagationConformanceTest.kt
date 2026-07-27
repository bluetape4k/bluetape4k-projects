package io.bluetape4k.ktor.observability

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
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapSetter
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentHashMap
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
class KtorContextPropagationConformanceTest {

    @Test
    fun `ktor extracts synthetic W3C parent and cleans request on success`() =
        TestTracing().use { tracing ->
            testApplication {
                installContextConformance(tracing)
                val captured = runKtorScenario(tracing, ContextPropagationScenario.SUCCESS)

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
    fun `ktor handler failure preserves parent and cleans request`() =
        TestTracing().use { tracing ->
            testApplication {
                installContextConformance(tracing)
                val captured = runKtorScenario(tracing, ContextPropagationScenario.FAILURE)

                check(captured.thrown?.javaClass == IllegalStateException::class.java)
                assertContextPropagationConformance(
                    captured.observation,
                    ktorExpectation(
                        ContextPropagationScenario.FAILURE,
                        ContextPropagationTerminal.FAILURE,
                    ),
                )
            }
        }

    @Test
    fun `ktor request cancellation preserves parent and cleans request`() =
        TestTracing().use { tracing ->
            testApplication {
                installContextConformance(tracing)
                val captured = runKtorScenario(tracing, ContextPropagationScenario.CANCELLATION)

                check(captured.thrown is CancellationException)
                check(captured.thrown !is TimeoutCancellationException)
                assertContextPropagationConformance(
                    captured.observation,
                    ktorExpectation(
                        ContextPropagationScenario.CANCELLATION,
                        ContextPropagationTerminal.CANCELLATION,
                    ),
                )
            }
        }

    @Test
    fun `ktor route deadline preserves parent and cleans request`() =
        TestTracing().use { tracing ->
            testApplication {
                installContextConformance(tracing)
                val captured = runKtorScenario(tracing, ContextPropagationScenario.DEADLINE)

                check(captured.thrown?.javaClass == TimeoutCancellationException::class.java)
                assertContextPropagationConformance(
                    captured.observation,
                    ktorExpectation(
                        ContextPropagationScenario.DEADLINE,
                        ContextPropagationTerminal.DEADLINE_EXCEEDED,
                    ),
                )
            }
        }

    @Test
    fun `ktor concurrent parents and unparented probe stay isolated`() =
        TestTracing().use { tracing ->
            testApplication {
                installContextConformance(tracing)
                assertContextIsolation(
                    runKtorIsolationScenario(tracing),
                    ktorIsolationExpectation(),
                )
            }
        }

    @Test
    fun `ktor isolation preserves pre-ready client failure identity`() {
        val failure = SyntheticKtorIsolationFailure(Any())
        TestTracing(failureBeforeRequestA = failure).use { tracing ->
            testApplication {
                installContextConformance(tracing)

                val thrown = assertFailsWith<SyntheticKtorIsolationFailure> {
                    runKtorIsolationScenario(tracing)
                }

                check(thrown === failure)
            }
        }
    }

    @Test
    fun `ktor isolation preserves post-release client failure identity`() {
        val failure = SyntheticKtorIsolationFailure(Any())
        TestTracing(failureAfterRequestA = failure).use { tracing ->
            testApplication {
                installContextConformance(tracing)

                val thrown = assertFailsWith<SyntheticKtorIsolationFailure> {
                    runKtorIsolationScenario(tracing)
                }

                check(thrown === failure)
            }
        }
    }

    @Test
    fun `ktor isolation releases peer when parent extraction fails before ready`() =
        TestTracing(omitIsolationParentA = true).use { tracing ->
            testApplication {
                installContextConformance(tracing)

                assertFailsWith<IllegalStateException> {
                    runKtorIsolationScenario(tracing)
                }
            }
        }
}

private const val traceIdA = "11111111111111111111111111111111"
private const val spanIdA = "1111111111111111"
private const val traceIdB = "22222222222222222222222222222222"
private const val spanIdB = "2222222222222222"
private val hangGuard = DEFAULT_CANCELLATION_CONTRACT_TIMEOUT
private val semanticDeadline = 250.milliseconds.also {
    check(it < hangGuard)
}

private class CapturedScenario(
    val observation: ContextPropagationObservation,
    val thrown: Throwable?,
)

private class SyntheticKtorIsolationFailure(
    @Suppress("unused")
    private val identityToken: Any,
): RuntimeException("synthetic Ktor isolation failure")

private enum class KtorConformanceEvent {
    READY,
    RELEASED,
    TERMINAL_OBSERVED,
    FINALLY_COMPLETED,
    CLEANUP_PROBED,
}

private class KtorConformanceEventEntry(
    val requestAlias: ContextRequestAlias,
    val event: KtorConformanceEvent,
    val sequence: Long,
)

private class KtorConformanceEventLedger {
    private val sequence = AtomicLong()
    private val events = ConcurrentLinkedQueue<KtorConformanceEventEntry>()

    fun record(
        requestAlias: ContextRequestAlias,
        event: KtorConformanceEvent,
    ) {
        events += KtorConformanceEventEntry(
            requestAlias,
            event,
            sequence.incrementAndGet(),
        )
    }

    fun assertSingleScenarioOrder() {
        val entries = events.filter { it.requestAlias == ContextRequestAlias.SINGLE }
        val counts = entries.groupingBy(KtorConformanceEventEntry::event).eachCount()
        listOf(
            KtorConformanceEvent.TERMINAL_OBSERVED,
            KtorConformanceEvent.FINALLY_COMPLETED,
            KtorConformanceEvent.CLEANUP_PROBED,
        ).forEach { event ->
            check(counts[event] == 1) {
                "Ktor lifecycle event count mismatch: alias=SINGLE, event=$event"
            }
        }
        check(counts[KtorConformanceEvent.READY] == null)
        check(counts[KtorConformanceEvent.RELEASED] == null)
        assertCleanupAfterFinally(ContextRequestAlias.SINGLE, entries)
    }

    fun assertIsolationOrder() {
        val snapshot = events.toList()
        val participantEntries = listOf(
            ContextRequestAlias.REQUEST_A,
            ContextRequestAlias.REQUEST_B,
        ).associateWith { alias ->
            snapshot.filter { it.requestAlias == alias }.also { entries ->
                val counts = entries.groupingBy(KtorConformanceEventEntry::event).eachCount()
                KtorConformanceEvent.entries.forEach { event ->
                    check(counts[event] == 1) {
                        "Ktor lifecycle event count mismatch: alias=$alias, event=$event"
                    }
                }
            }
        }
        val lastReady = participantEntries.values
            .flatten()
            .filter { it.event == KtorConformanceEvent.READY }
            .maxOf(KtorConformanceEventEntry::sequence)
        val firstRelease = participantEntries.values
            .flatten()
            .filter { it.event == KtorConformanceEvent.RELEASED }
            .minOf(KtorConformanceEventEntry::sequence)
        check(lastReady < firstRelease)
        participantEntries.forEach { (alias, entries) ->
            assertCleanupAfterFinally(alias, entries)
        }
    }

    private fun assertCleanupAfterFinally(
        alias: ContextRequestAlias,
        entries: List<KtorConformanceEventEntry>,
    ) {
        val terminalSequence = entries.single {
            it.event == KtorConformanceEvent.TERMINAL_OBSERVED
        }.sequence
        val finallySequence = entries.single {
            it.event == KtorConformanceEvent.FINALLY_COMPLETED
        }.sequence
        val cleanupSequence = entries.single {
            it.event == KtorConformanceEvent.CLEANUP_PROBED
        }.sequence
        check(terminalSequence < cleanupSequence) {
            "Ktor terminal cleanup order mismatch: alias=$alias"
        }
        check(finallySequence < cleanupSequence) {
            "Ktor lifecycle cleanup order mismatch: alias=$alias"
        }
    }
}

private class KtorConformanceHarness {
    val singleObservations = ConcurrentLinkedQueue<ContextMarkerObservation>()
    val singleStarted = CompletableDeferred<Unit>()
    val singleFinally = CompletableDeferred<Unit>()
    val singleThrown = AtomicReference<Throwable?>()
    val isolationObservations = ConcurrentHashMap<ContextRequestAlias, ConcurrentLinkedQueue<String?>>()
    val readyA = CompletableDeferred<Unit>()
    val readyB = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    val finallyA = CompletableDeferred<Unit>()
    val finallyB = CompletableDeferred<Unit>()
    val firstIsolationFailure = AtomicReference<Throwable?>()
    val probeTraceId = CompletableDeferred<String?>()
    val ledger = KtorConformanceEventLedger()

    fun observations(alias: ContextRequestAlias): ConcurrentLinkedQueue<String?> =
        isolationObservations.computeIfAbsent(alias) {
            ConcurrentLinkedQueue()
        }

    fun releaseAll() {
        readyA.complete(Unit)
        readyB.complete(Unit)
        release.complete(Unit)
    }
}

private class TestTracing(
    val omitIsolationParentA: Boolean = false,
    val failureBeforeRequestA: Throwable? = null,
    val failureAfterRequestA: Throwable? = null,
) : AutoCloseable {
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

    val harness = KtorConformanceHarness()

    fun headers(traceId: String, spanId: String): Map<String, String> {
        val carrier = mutableMapOf<String, String>()
        val parent =
            Span.wrap(
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
            TextMapSetter<MutableMap<String, String>> { target, key, value ->
                target?.set(key, value)
            },
        )
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

private fun ApplicationTestBuilder.installContextConformance(tracing: TestTracing) {
    application {
        installBluetape4kKtorOpenTelemetryTracing(
            KtorOpenTelemetryTracingConfig(
                openTelemetry = tracing.openTelemetry,
            ),
        )
        contextRoutes(tracing.harness)
    }
}

private fun Application.contextRoutes(harness: KtorConformanceHarness) {
    routing {
        get("/context/probe") {
            val traceId = Span.current().validTraceIdOrNull()
            harness.probeTraceId.complete(traceId)
            call.respond(HttpStatusCode.OK)
        }
        get("/context/{scenario}") {
            val scenario =
                ContextPropagationScenario.valueOf(
                    requireNotNull(call.parameters["scenario"]),
                )
            if (scenario == ContextPropagationScenario.ISOLATION) {
                runIsolationRoute(harness)
            } else {
                runSingleRoute(harness, scenario)
            }
        }
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.runSingleRoute(
    harness: KtorConformanceHarness,
    scenario: ContextPropagationScenario,
) {
    try {
        harness.singleObservations += markerObservation(
            ContextObservationPoint.BOUNDARY_ENTER,
        )
        harness.singleStarted.complete(Unit)
        yield()
        harness.singleObservations += markerObservation(
            ContextObservationPoint.AFTER_SUSPENSION,
        )
        harness.singleObservations += markerObservation(
            ContextObservationPoint.BEFORE_TERMINAL,
        )
        when (scenario) {
            ContextPropagationScenario.SUCCESS ->
                call.respond(HttpStatusCode.OK)

            ContextPropagationScenario.FAILURE ->
                error("synthetic Ktor handler failure")

            ContextPropagationScenario.CANCELLATION -> {
                currentCoroutineContext().job.cancel(
                    CancellationException("synthetic Ktor request cancellation"),
                )
                yield()
            }

            ContextPropagationScenario.DEADLINE ->
                withTimeout(semanticDeadline) {
                    awaitCancellation()
                }

            ContextPropagationScenario.ISOLATION ->
                error("Isolation uses runIsolationRoute")
        }
    } catch (failure: Throwable) {
        harness.singleThrown.compareAndSet(null, failure)
        throw failure
    } finally {
        harness.singleFinally.complete(Unit)
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.runIsolationRoute(
    harness: KtorConformanceHarness,
) {
    var ownFinally: CompletableDeferred<Unit>? = null
    try {
        val traceId = Span.current().validTraceIdOrNull()
        val alias =
            when (traceId) {
                traceIdA -> ContextRequestAlias.REQUEST_A
                traceIdB -> ContextRequestAlias.REQUEST_B
                else -> error("Unexpected synthetic isolation parent")
            }
        val ownReady =
            when (alias) {
                ContextRequestAlias.REQUEST_A -> harness.readyA
                ContextRequestAlias.REQUEST_B -> harness.readyB
                else -> error("Unexpected isolation alias")
            }
        ownFinally =
            when (alias) {
                ContextRequestAlias.REQUEST_A -> harness.finallyA
                ContextRequestAlias.REQUEST_B -> harness.finallyB
                else -> error("Unexpected isolation alias")
            }
        harness.observations(alias) += Span.current().validTraceIdOrNull()
        harness.ledger.record(alias, KtorConformanceEvent.READY)
        ownReady.complete(Unit)
        harness.readyA.awaitGateWithin("isolation ready A")
        harness.readyB.awaitGateWithin("isolation ready B")
        harness.release.awaitGateWithin("isolation release")
        harness.ledger.record(alias, KtorConformanceEvent.RELEASED)
        yield()
        harness.observations(alias) += Span.current().validTraceIdOrNull()
        harness.observations(alias) += Span.current().validTraceIdOrNull()
        call.respond(HttpStatusCode.OK)
    } catch (failure: Throwable) {
        harness.firstIsolationFailure.compareAndSet(null, failure)
        harness.releaseAll()
        throw failure
    } finally {
        harness.releaseAll()
        ownFinally?.complete(Unit)
    }
}

private fun markerObservation(point: ContextObservationPoint): ContextMarkerObservation =
    ContextMarkerObservation(
        point = point,
        observedMarker = Span.current().validTraceIdOrNull(),
    )

private fun Span.validTraceIdOrNull(): String? =
    spanContext.takeIf(SpanContext::isValid)?.traceId

private suspend fun ApplicationTestBuilder.runKtorScenario(
    tracing: TestTracing,
    scenario: ContextPropagationScenario,
): CapturedScenario =
    supervisorScope {
        check(scenario != ContextPropagationScenario.ISOLATION) {
            "Isolation uses runKtorIsolationScenario"
        }
        val harness = tracing.harness
        val requests = mutableListOf<Deferred<*>>()
        var primaryFailure: Throwable? = null
        try {
            val request = async {
                client.get("/context/$scenario") {
                    headers {
                        tracing.headers(traceIdA, spanIdA).forEach { (name, value) ->
                            append(name, value)
                        }
                    }
                }
            }
            requests += request
            harness.singleStarted.awaitGateWithin("single route start")
            request.awaitTerminalWithin("single request terminal")
            harness.ledger.record(
                ContextRequestAlias.SINGLE,
                KtorConformanceEvent.TERMINAL_OBSERVED,
            )
            harness.singleFinally.awaitGateWithin("single route finally")
            harness.ledger.record(
                ContextRequestAlias.SINGLE,
                KtorConformanceEvent.FINALLY_COMPLETED,
            )
            val requestMarker = Span.current().validTraceIdOrNull()
            harness.ledger.record(
                ContextRequestAlias.SINGLE,
                KtorConformanceEvent.CLEANUP_PROBED,
            )
            harness.ledger.assertSingleScenarioOrder()

            CapturedScenario(
                observation =
                    ContextPropagationObservation(
                        boundary = ContextPropagationBoundary.KTOR_REQUEST,
                        scenario = scenario,
                        requestAlias = ContextRequestAlias.SINGLE,
                        markerObservations = harness.singleObservations.toList(),
                        cleanupProbes = listOf(
                            ContextCleanupProbe(
                                ContextProbeLocation.REQUEST,
                                requestMarker,
                            ),
                        ),
                        terminal = terminalFor(scenario),
                    ),
                thrown = harness.singleThrown.get(),
            )
        } catch (failure: Throwable) {
            primaryFailure = failure
            throw failure
        } finally {
            cleanupKtorRequests(primaryFailure, harness, requests)
        }
    }

private suspend fun ApplicationTestBuilder.runKtorIsolationScenario(
    tracing: TestTracing,
): ContextIsolationObservation =
    supervisorScope {
        val harness = tracing.harness
        val requests = mutableListOf<Deferred<*>>()
        var primaryFailure: Throwable? = null
        try {
            val requestA = async {
                runIsolationClientRequest(
                    harness = harness,
                    failureBeforeRequest = tracing.failureBeforeRequestA,
                    failureAfterRequest = tracing.failureAfterRequestA,
                ) {
                    client.get("/context/${ContextPropagationScenario.ISOLATION}") {
                        headers {
                            val parentHeaders =
                                if (tracing.omitIsolationParentA) {
                                    emptyMap()
                                } else {
                                    tracing.headers(traceIdA, spanIdA)
                                }
                            parentHeaders.forEach { (name, value) ->
                                append(name, value)
                            }
                        }
                    }
                }
            }
            val requestB = async {
                runIsolationClientRequest(harness) {
                    client.get("/context/${ContextPropagationScenario.ISOLATION}") {
                        headers {
                            tracing.headers(traceIdB, spanIdB).forEach { (name, value) ->
                                append(name, value)
                            }
                        }
                    }
                }
            }
            requests += requestA
            requests += requestB

            harness.readyA.awaitGateWithin("request A ready")
            harness.readyB.awaitGateWithin("request B ready")
            harness.firstIsolationFailure.get()?.let { throw it }
            harness.release.complete(Unit)

            val failureA = requestA.captureTerminalWithin("request A terminal")
            harness.ledger.record(
                ContextRequestAlias.REQUEST_A,
                KtorConformanceEvent.TERMINAL_OBSERVED,
            )
            val failureB = requestB.captureTerminalWithin("request B terminal")
            harness.ledger.record(
                ContextRequestAlias.REQUEST_B,
                KtorConformanceEvent.TERMINAL_OBSERVED,
            )
            (harness.firstIsolationFailure.get() ?: failureA ?: failureB)?.let { throw it }

            harness.finallyA.awaitGateWithin("request A finally")
            harness.ledger.record(
                ContextRequestAlias.REQUEST_A,
                KtorConformanceEvent.FINALLY_COMPLETED,
            )
            harness.finallyB.awaitGateWithin("request B finally")
            harness.ledger.record(
                ContextRequestAlias.REQUEST_B,
                KtorConformanceEvent.FINALLY_COMPLETED,
            )

            val requestMarker = Span.current().validTraceIdOrNull()
            harness.ledger.record(
                ContextRequestAlias.REQUEST_A,
                KtorConformanceEvent.CLEANUP_PROBED,
            )
            harness.ledger.record(
                ContextRequestAlias.REQUEST_B,
                KtorConformanceEvent.CLEANUP_PROBED,
            )

            withTimeout(hangGuard) {
                client.get("/context/probe")
            }
            val probeMarker = harness.probeTraceId.awaitGateWithin("unparented probe")
            harness.ledger.assertIsolationOrder()

            ContextIsolationObservation(
                boundary = ContextPropagationBoundary.KTOR_REQUEST,
                samples = listOf(
                    ContextIsolationSample(
                        ContextRequestAlias.REQUEST_A,
                        harness.observations(ContextRequestAlias.REQUEST_A).toList(),
                    ),
                    ContextIsolationSample(
                        ContextRequestAlias.REQUEST_B,
                        harness.observations(ContextRequestAlias.REQUEST_B).toList(),
                    ),
                    ContextIsolationSample(
                        ContextRequestAlias.PROBE,
                        listOf(probeMarker),
                    ),
                ),
                cleanupProbes = listOf(
                    ContextCleanupProbe(
                        ContextProbeLocation.REQUEST,
                        requestMarker,
                    ),
                ),
            )
        } catch (failure: Throwable) {
            primaryFailure = failure
            throw failure
        } finally {
            cleanupKtorRequests(primaryFailure, harness, requests)
        }
    }

private suspend fun runIsolationClientRequest(
    harness: KtorConformanceHarness,
    failureBeforeRequest: Throwable? = null,
    failureAfterRequest: Throwable? = null,
    request: suspend () -> Unit,
) {
    try {
        failureBeforeRequest?.let { throw it }
        request()
        failureAfterRequest?.let { throw it }
    } catch (failure: Throwable) {
        harness.firstIsolationFailure.compareAndSet(null, failure)
        harness.releaseAll()
        throw failure
    }
}

private suspend fun cleanupKtorRequests(
    primaryFailure: Throwable?,
    harness: KtorConformanceHarness,
    requests: List<Deferred<*>>,
) {
    var aggregatedFailure = primaryFailure
    withContext(NonCancellable) {
        val actions = buildList<suspend () -> Unit> {
            add(harness::releaseAll)
            requests.forEach { request ->
                add {
                    request.cancel(CancellationException("Ktor conformance cleanup"))
                }
            }
            add {
                try {
                    withTimeout(hangGuard) {
                        requests.joinAll()
                    }
                } catch (failure: TimeoutCancellationException) {
                    throw AssertionError("Timed out cleaning Ktor conformance requests", failure)
                }
            }
        }
        actions.forEach { action ->
            val cleanupFailure =
                try {
                    action()
                    null
                } catch (failure: Throwable) {
                    failure
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

private suspend fun <T> CompletableDeferred<T>.awaitGateWithin(label: String): T =
    try {
        withTimeout(hangGuard) {
            await()
        }
    } catch (failure: TimeoutCancellationException) {
        throw AssertionError("Timed out waiting for Ktor $label", failure)
    }

private suspend fun Deferred<*>.awaitTerminalWithin(label: String) {
    try {
        withTimeout(hangGuard) {
            await()
        }
    } catch (failure: TimeoutCancellationException) {
        if (!isCompleted) {
            throw AssertionError("Timed out waiting for Ktor $label", failure)
        }
        throw failure
    }
}

private suspend fun Deferred<*>.captureTerminalWithin(label: String): Throwable? =
    try {
        withTimeout(hangGuard) {
            await()
        }
        null
    } catch (failure: TimeoutCancellationException) {
        if (isCompleted) {
            failure
        } else {
            throw AssertionError("Timed out waiting for Ktor $label", failure)
        }
    } catch (failure: CancellationException) {
        failure
    } catch (failure: Exception) {
        failure
    } catch (failure: Error) {
        failure
    }

private fun ktorExpectation(
    scenario: ContextPropagationScenario,
    terminal: ContextPropagationTerminal,
): ContextPropagationExpectation =
    ContextPropagationExpectation(
        boundary = ContextPropagationBoundary.KTOR_REQUEST,
        scenario = scenario,
        requestAlias = ContextRequestAlias.SINGLE,
        markerExpectations = listOf(
            ContextMarkerExpectation(ContextObservationPoint.BOUNDARY_ENTER, traceIdA),
            ContextMarkerExpectation(ContextObservationPoint.AFTER_SUSPENSION, traceIdA),
            ContextMarkerExpectation(ContextObservationPoint.BEFORE_TERMINAL, traceIdA),
        ),
        cleanupExpectations = listOf(
            ContextCleanupExpectation(ContextProbeLocation.REQUEST, null),
        ),
        expectedTerminal = terminal,
    )

private fun ktorIsolationExpectation(): ContextIsolationExpectation =
    ContextIsolationExpectation(
        boundary = ContextPropagationBoundary.KTOR_REQUEST,
        samples = listOf(
            ContextIsolationSampleExpectation(
                requestAlias = ContextRequestAlias.REQUEST_A,
                mode = ContextMarkerExpectationMode.EXACT,
                expectedMarker = traceIdA,
                minimumObservationCount = 3,
            ),
            ContextIsolationSampleExpectation(
                requestAlias = ContextRequestAlias.REQUEST_B,
                mode = ContextMarkerExpectationMode.EXACT,
                expectedMarker = traceIdB,
                minimumObservationCount = 3,
            ),
            ContextIsolationSampleExpectation(
                requestAlias = ContextRequestAlias.PROBE,
                mode = ContextMarkerExpectationMode.NOT_IN,
                forbiddenMarkers = listOf(traceIdA, traceIdB),
            ),
        ),
        cleanupExpectations = listOf(
            ContextCleanupExpectation(ContextProbeLocation.REQUEST, null),
        ),
    )

private fun terminalFor(
    scenario: ContextPropagationScenario,
): ContextPropagationTerminal =
    when (scenario) {
        ContextPropagationScenario.SUCCESS -> ContextPropagationTerminal.SUCCESS
        ContextPropagationScenario.FAILURE -> ContextPropagationTerminal.FAILURE
        ContextPropagationScenario.CANCELLATION -> ContextPropagationTerminal.CANCELLATION
        ContextPropagationScenario.DEADLINE -> ContextPropagationTerminal.DEADLINE_EXCEEDED
        ContextPropagationScenario.ISOLATION -> error("Isolation does not have a single terminal")
    }
