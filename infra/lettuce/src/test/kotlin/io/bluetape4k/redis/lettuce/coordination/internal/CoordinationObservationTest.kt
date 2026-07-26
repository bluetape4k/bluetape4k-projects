package io.bluetape4k.redis.lettuce.coordination.internal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBePositive
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.redis.lettuce.script.RedisScript
import io.lettuce.core.RedisNoScriptException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicInteger

class CoordinationObservationTest {

    @Test
    fun `dimensions accept only low-cardinality allowlisted labels`() {
        val dimensions = CoordinationDimensions.of(
            "object_kind" to "lock",
            "operation" to "acquire",
            "outcome" to "success",
            "failure_kind" to "none",
            "lease_policy" to "watchdog",
        )

        dimensions.size shouldBeEqualTo 5
        assertFailsWith<IllegalArgumentException> {
            CoordinationDimensions.of("owner_id" to "raw-owner")
        }
        assertFailsWith<IllegalArgumentException> {
            CoordinationDimensions.of("operation" to "customer-${"x".repeat(80)}")
        }
    }

    @Test
    fun `observation catalog covers every required counter gauge histogram and event`() {
        val catalog = CoordinationObservationName.entries.associateBy { it.wireName }
        val emitted = mutableListOf<CoordinationObservation>()
        val observer = CoordinationObserver(emitted::add)
        val expected = mapOf(
            "coordination.operation.outcome" to CoordinationObservationKind.COUNTER,
            "coordination.reconciliation" to CoordinationObservationKind.COUNTER,
            "coordination.stale.cleanup" to CoordinationObservationKind.COUNTER,
            "coordination.cleanup.pending" to CoordinationObservationKind.EVENT,
            "coordination.ownership.loss" to CoordinationObservationKind.COUNTER,
            "coordination.watchdog.late" to CoordinationObservationKind.COUNTER,
            "coordination.watchdog.missed" to CoordinationObservationKind.COUNTER,
            "coordination.noscript.fallback" to CoordinationObservationKind.COUNTER,
            "coordination.integrity.rejection" to CoordinationObservationKind.COUNTER,
            "coordination.capacity.rejection" to CoordinationObservationKind.COUNTER,
            "coordination.watchdogs.active" to CoordinationObservationKind.GAUGE,
            "coordination.backlog.due" to CoordinationObservationKind.GAUGE,
            "coordination.tasks.active" to CoordinationObservationKind.GAUGE,
            "coordination.waiters" to CoordinationObservationKind.GAUGE,
            "coordination.objects" to CoordinationObservationKind.GAUGE,
            "coordination.request.holds" to CoordinationObservationKind.GAUGE,
            "coordination.redis.latency" to CoordinationObservationKind.HISTOGRAM,
            "coordination.wait.latency" to CoordinationObservationKind.HISTOGRAM,
            "coordination.retries" to CoordinationObservationKind.COUNTER,
            "coordination.cleanup.batch.size" to CoordinationObservationKind.HISTOGRAM,
        )

        expected.forEach { (name, kind) -> catalog.getValue(name).kind shouldBeEqualTo kind }
        CoordinationObservationName.entries.forEach { name -> observer.emit(name, value = 2.0) }
        emitted.map { it.name }.toSet() shouldBeEqualTo CoordinationObservationName.entries.toSet()
        emitted.all { it.value == 2.0 }.shouldBeTrue()
    }

    @Test
    fun `risk catalog maps every required signal to observations and alert predicates`() {
        val signals = CoordinationRiskSignal.entries.associateBy { it.wireName }
        val expected = setOf(
            "queued-waiters-not-decreasing",
            "post-close-activity",
            "ownership-loss-increment",
            "cleanup-pending-persistence",
            "capacity-rejection",
            "integrity-or-noscript-spike",
            "active-waiters-without-progress",
        )

        signals.keys shouldBeEqualTo expected
        signals.values.all { it.observations.isNotEmpty() && it.alertPredicate.isNotBlank() }.shouldBeTrue()
    }

    @Test
    fun `throwing sink cannot change result mutation count or task registration`() {
        val mutationCount = AtomicInteger()
        val registrationCount = AtomicInteger()
        val observer = CoordinationObserver { throw IllegalStateException("sink failed") }

        val result = observer.observe(
            name = CoordinationObservationName.OPERATION_OUTCOME,
            dimensions = CoordinationDimensions.of("object_kind" to "lock", "operation" to "acquire"),
        ) {
            mutationCount.incrementAndGet()
            registrationCount.incrementAndGet()
            "acquired"
        }

        result shouldBeEqualTo "acquired"
        mutationCount.get() shouldBeEqualTo 1
        registrationCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `script bridge reuses NOSCRIPT fallback and a throwing sink cannot duplicate Redis mutation`() {
        val commands = mockk<RedisScriptingCommands<String, String>>()
        val script = RedisScript("return ARGV[1]")
        val keys = arrayOf("bt4k:coord:v1:{inventory}:lock:inventory:state")
        val mutationCount = AtomicInteger()
        every {
            commands.evalsha<String>(script.sha1, ScriptOutputType.VALUE, keys, "value")
        } throws RedisNoScriptException("NOSCRIPT")
        every {
            commands.eval<String>(script.source, ScriptOutputType.VALUE, keys, "value")
        } answers {
            mutationCount.incrementAndGet()
            "value"
        }
        val executor = CoordinationScriptExecutor(
            CoordinationObserver { throw IllegalStateException("sink failed") },
        )

        executor.run<String>(
            commands = commands,
            script = script,
            outputType = ScriptOutputType.VALUE,
            keys = keys,
            args = arrayOf("value"),
        ) shouldBeEqualTo "value"

        mutationCount.get() shouldBeEqualTo 1
        verify(exactly = 1) {
            commands.evalsha<String>(script.sha1, ScriptOutputType.VALUE, keys, "value")
            commands.eval<String>(script.source, ScriptOutputType.VALUE, keys, "value")
        }
        confirmVerified(commands)
    }

    @Test
    fun `throwing sink preserves exception cancellation and future identity`() = runTest {
        val observer = CoordinationObserver { throw IllegalStateException("sink failed") }
        val expectedFailure = IllegalStateException("backend")
        val failure = assertFailsWith<IllegalStateException> {
            observer.observe(
                CoordinationObservationName.OPERATION_OUTCOME,
                CoordinationDimensions.EMPTY,
            ) {
                throw expectedFailure
            }
        }
        failure.shouldBeSameInstanceAs(expectedFailure)

        val expectedCancellation = CancellationException("cancelled")
        val cancellation = assertFailsWith<CancellationException> {
            observer.observeSuspending(
                CoordinationObservationName.OPERATION_OUTCOME,
                CoordinationDimensions.EMPTY,
            ) {
                throw expectedCancellation
            }
        }
        cancellation.shouldBeSameInstanceAs(expectedCancellation)

        val future = CompletableFuture<String>()
        observer.observeFuture(
            CoordinationObservationName.OPERATION_OUTCOME,
            CoordinationDimensions.EMPTY,
            future,
        ).shouldBeSameInstanceAs(future)
        future.completeExceptionally(expectedFailure)
        val futureFailure = assertFailsWith<CompletionException> { future.join() }
        futureFailure.cause.shouldBeSameInstanceAs(expectedFailure)

        val cancelled = CompletableFuture<String>()
        observer.observeFuture(
            CoordinationObservationName.OPERATION_OUTCOME,
            CoordinationDimensions.EMPTY,
            cancelled,
        )
        cancelled.cancel(false).shouldBeTrue()
        cancelled.isCancelled.shouldBeTrue()
        observer.droppedObservations.shouldBePositive()
        observer.emittedObservations.shouldBePositive()
    }
}
