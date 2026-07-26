package io.bluetape4k.junit5.http.idempotency

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class BoundedWaitHttpIdempotencyConformanceLifecycleTest {

    @Test
    fun `runner completes scenarios and releases its watchdog`() = runSuspendIO {
        val adapter = RecordingAdapter()

        runConformanceScenarios(
            adapter = adapter,
            config = config(),
            scenarios = listOf(
                ConformanceScenario("success") { target, limits ->
                    exchangeChecked(target, limits, request()) shouldBeEqualTo createdResponse()
                },
            ),
        )

        adapter.exchangeCount.get() shouldBeEqualTo 1
        adapter.resetCount.get() shouldBeEqualTo 2
        liveWatchdogThreadCount() shouldBeEqualTo 0
    }

    @Test
    fun `stalled scenario is cancelled with redacted timeout and cleanup runs`() = runSuspendIO {
        val adapter = RecordingAdapter(exchangeBlock = {
            try {
                awaitCancellation()
            } finally {
                cancelledExchangeCount.incrementAndGet()
            }
        })

        val failure = assertFailsWith<AssertionError> {
            runConformanceScenarios(
                adapter = adapter,
                config = config(scenarioTimeout = Duration.ofSeconds(1)),
                scenarios = listOf(
                    ConformanceScenario("first-request") { target, limits ->
                        exchangeChecked(
                            target,
                            limits,
                            request(idempotencyKeys = listOf("key-secret"), requestBody = "body-secret"),
                        )
                    },
                ),
            )
        }

        failure.message.orEmpty() shouldContain "scenario=first-request"
        throwableText(failure) shouldNotContain "key-secret"
        throwableText(failure) shouldNotContain "body-secret"
        adapter.cancelledExchangeCount.get() shouldBeEqualTo 1
        adapter.childStartedCount.get() shouldBeEqualTo 1
        adapter.resetCount.get() shouldBeEqualTo 2
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
        liveWatchdogThreadCount() shouldBeEqualTo 0
    }

    @Test
    fun `watchdog is armed before guarded work reaches its first suspension`() = runSuspendIO {
        val guardedWorkStarted = CompletableDeferred<Unit>()

        val run = async {
            assertFailsWith<AssertionError> {
                runConformanceScenarios(
                    adapter = RecordingAdapter(),
                    config = config(scenarioTimeout = Duration.ofSeconds(1)),
                    scenarios = listOf(
                        ConformanceScenario("pre-suspension-stall") { _, _ ->
                            guardedWorkStarted.complete(Unit)
                            Thread.sleep(1_200)
                            awaitCancellation()
                        },
                    ),
                )
            }
        }

        guardedWorkStarted.await()
        liveWatchdogThreadCount() shouldBeEqualTo 1
        val failure = run.await()
        failure.message.orEmpty() shouldContain "scenario=pre-suspension-stall"
        liveWatchdogThreadCount() shouldBeEqualTo 0
    }

    @Test
    fun `caller cancellation cancels work and still performs bounded cleanup`() = runSuspendIO {
        val started = CompletableDeferred<Unit>()
        val adapter = RecordingAdapter(exchangeBlock = {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelledExchangeCount.incrementAndGet()
            }
        })
        val runner = launch {
            runConformanceScenarios(
                adapter,
                config(),
                listOf(ConformanceScenario("cancelled") { target, limits ->
                    exchangeChecked(target, limits, request())
                }),
            )
        }

        started.await()
        runner.cancelAndJoin()

        adapter.cancelledExchangeCount.get() shouldBeEqualTo 1
        adapter.childStartedCount.get() shouldBeEqualTo 1
        adapter.resetCount.get() shouldBeEqualTo 2
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
        liveWatchdogThreadCount() shouldBeEqualTo 0
    }

    @Test
    fun `scenario failure remains primary and cleanup failure is suppressed`() = runSuspendIO {
        val adapter = RecordingAdapter(resetBlock = { throw IllegalStateException("cleanup-redacted") })

        val failure = assertFailsWith<IllegalArgumentException> {
            runConformanceScenarios(
                adapter,
                config(),
                listOf(ConformanceScenario("primary") { _, _ ->
                    throw IllegalArgumentException("primary-redacted")
                }),
            )
        }

        failure.message shouldBeEqualTo "primary-redacted"
        failure.suppressed.map { it.message } shouldBeEqualTo listOf("cleanup-redacted", "cleanup-redacted")
        liveWatchdogThreadCount() shouldBeEqualTo 0
    }

    @Test
    fun `same scenario and cleanup failure instance preserves primary identity`() = runSuspendIO {
        val shared = SharedFailure(1)
        val adapter = RecordingAdapter(resetBlock = { throw shared })

        val failure = assertFailsWith<SharedFailure> {
            runConformanceScenarios(
                adapter,
                config(),
                listOf(ConformanceScenario("shared-failure") { _, _ -> throw shared }),
            )
        }

        failure shouldBeSameInstanceAs shared
        failure.suppressed.shouldBeEmpty()
        liveWatchdogThreadCount() shouldBeEqualTo 0
    }

    @Test
    fun `stalled reset is bounded independently from caller cancellation`() = runSuspendIO {
        val adapter = RecordingAdapter(resetBlock = { awaitCancellation() })

        val failure = assertFailsWith<AssertionError> {
            runConformanceScenarios(
                adapter,
                config(scenarioTimeout = Duration.ofSeconds(1)),
                listOf(ConformanceScenario("success") { _, _ -> }),
            )
        }

        failure.message.orEmpty() shouldContain "scenario=success-cleanup"
        failure.suppressed.single().message.orEmpty() shouldContain "scenario=final-cleanup"
        liveWatchdogThreadCount() shouldBeEqualTo 0
    }

    @Test
    fun `quiescence mismatch fails scenario cleanup`() = runSuspendIO {
        val adapter = RecordingAdapter(quiescenceValue = HttpIdempotencyQuiescence(1, 0, 0))

        val failure = assertFailsWith<AssertionError> {
            runConformanceScenarios(
                adapter,
                config(),
                listOf(ConformanceScenario("leaked-waiter") { _, _ -> }),
            )
        }

        failure.message.orEmpty() shouldContain "HttpIdempotencyQuiescence"
        liveWatchdogThreadCount() shouldBeEqualTo 0
    }

    @Test
    fun `runner rejects fan-in workload above proof budget before exchange`() = runSuspendIO {
        val accepted = RecordingAdapter()
        runConformanceScenarios(accepted, config(maxWaitersPerKey = 32), emptyList())

        val rejected = RecordingAdapter()
        assertFailsWith<IllegalArgumentException> {
            runConformanceScenarios(
                rejected,
                config(maxWaitersPerKey = 33),
                listOf(ConformanceScenario("never-runs") { target, limits ->
                    exchangeChecked(target, limits, request())
                }),
            )
        }

        rejected.exchangeCount.get() shouldBeEqualTo 0
        rejected.resetCount.get() shouldBeEqualTo 0
    }

    @Test
    fun `fixture preflight rejects too small valid limits before adapter invocation`() = runSuspendIO {
        val adapter = RecordingAdapter()

        val failure = assertFailsWith<IllegalArgumentException> {
            runConformanceScenarios(
                adapter,
                config(maxIdempotencyKeyBytes = 1, maxRequestBodyBytes = 1),
                listOf(ConformanceScenario("never-runs") { target, limits ->
                    exchangeChecked(target, limits, request())
                }),
            )
        }

        throwableText(failure) shouldNotContain "fixture-key"
        adapter.exchangeCount.get() shouldBeEqualTo 0
        adapter.resetCount.get() shouldBeEqualTo 0
    }

    @Test
    fun `response bounds are always enforced without exposing response content`() = runSuspendIO {
        val adapter = RecordingAdapter(
            response = HttpIdempotencyResponse(
                statusCode = 201,
                body = "response-secret".repeat(10),
                headers = mapOf("content-type" to listOf("application/json")),
            ),
        )

        val failure = assertFailsWith<IllegalArgumentException> {
            runConformanceScenarios(
                adapter,
                config(maxReplayBodyBytes = 64),
                listOf(ConformanceScenario("bounded-response") { target, limits ->
                    exchangeChecked(target, limits, request())
                }),
            )
        }

        throwableText(failure) shouldNotContain "response-secret"
        adapter.exchangeCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `configured replay header aggregate fails at the breached value`() = runSuspendIO {
        val adapter = RecordingAdapter(
            response = HttpIdempotencyResponse(
                statusCode = 201,
                body = "{}",
                headers = mapOf(
                    "content-type" to listOf("application/json"),
                    "x-proof" to listOf("header-secret".repeat(8)),
                ),
            ),
        )

        val failure = assertFailsWith<IllegalArgumentException> {
            runConformanceScenarios(
                adapter,
                config(
                    maxReplayHeaderBytes = 64,
                    replayHeaderAllowlist = setOf("x-proof"),
                ),
                listOf(ConformanceScenario("bounded-headers") { target, limits ->
                    exchangeChecked(target, limits, request())
                }),
            )
        }

        failure.message.orEmpty() shouldContain "aggregate"
        throwableText(failure) shouldNotContain "header-secret"
        adapter.exchangeCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `reset remains a cooperative suspend boundary`() = runSuspendIO {
        val resetStarted = CompletableDeferred<Unit>()
        val adapter = RecordingAdapter(resetBlock = {
            resetStarted.complete(Unit)
            awaitCancellation()
        })
        val reset = launch { adapter.resetScenario() }

        resetStarted.await()
        reset.cancelAndJoin()

        reset.isCancelled.shouldBeTrue()
    }

    private class RecordingAdapter(
        private val response: HttpIdempotencyResponse = createdResponse(),
        private val exchangeBlock: (suspend RecordingAdapter.() -> HttpIdempotencyResponse)? = null,
        private val resetBlock: (suspend RecordingAdapter.() -> Unit)? = null,
        private val quiescenceValue: HttpIdempotencyQuiescence = HttpIdempotencyQuiescence(0, 0, 0),
    ): BoundedWaitHttpIdempotencyAdapter {

        val exchangeCount = AtomicInteger()
        val resetCount = AtomicInteger()
        val cancelledExchangeCount = AtomicInteger()
        val childStartedCount = AtomicInteger()
        private val activeChildren: MutableSet<Job> = Collections.synchronizedSet(mutableSetOf())

        override suspend fun exchange(request: HttpIdempotencyRequest): HttpIdempotencyResponse = coroutineScope {
            exchangeCount.incrementAndGet()
            val child = launch(start = CoroutineStart.UNDISPATCHED) {
                childStartedCount.incrementAndGet()
                awaitCancellation()
            }
            activeChildren.add(child)
            try {
                exchangeBlock?.invoke(this@RecordingAdapter) ?: response
            } finally {
                child.cancelAndJoin()
                activeChildren.remove(child)
            }
        }

        override suspend fun awaitOwnerStarted(request: HttpIdempotencyRequest) = Unit
        override suspend fun awaitWaiterCount(request: HttpIdempotencyRequest, expected: Int) = Unit

        override suspend fun completeOwner(
            request: HttpIdempotencyRequest,
            outcome: HttpIdempotencyResponse,
        ) = Unit

        override suspend fun holdOwnerResponseDelivery(request: HttpIdempotencyRequest) = Unit

        override suspend fun releaseOwnerResponseDelivery(request: HttpIdempotencyRequest) = Unit

        override suspend fun abandonOwner(
            request: HttpIdempotencyRequest,
            outcome: HttpIdempotencyResponse,
        ) = Unit

        override suspend fun advanceTimeBy(duration: Duration) = Unit

        override suspend fun resetScenario() {
            resetCount.incrementAndGet()
            activeChildrenSnapshot().forEach { child -> child.cancelAndJoin() }
            resetBlock?.invoke(this)
        }

        override fun sideEffectCount(request: HttpIdempotencyRequest): Int = 0

        override fun quiescence(): HttpIdempotencyQuiescence = quiescenceValue.copy(
            activeChildTasks = quiescenceValue.activeChildTasks + activeChildrenSnapshot().count(Job::isActive),
        )

        private fun activeChildrenSnapshot(): List<Job> = synchronized(activeChildren) {
            activeChildren.toList()
        }
    }

    private class SharedFailure(
        @Suppress("UNUSED_PARAMETER") marker: Int,
    ): RuntimeException("shared-redacted")

    private fun liveWatchdogThreadCount(): Int =
        Thread.getAllStackTraces().keys.count { it.isAlive && it.name == "http-idempotency-watchdog" }

    private fun throwableText(failure: Throwable): String = buildString {
        val visited = mutableSetOf<Throwable>()
        fun appendFailure(current: Throwable?) {
            if (current == null || !visited.add(current)) return
            append(current.message.orEmpty())
            current.suppressed.forEach(::appendFailure)
            appendFailure(current.cause)
        }
        appendFailure(failure)
    }
}
