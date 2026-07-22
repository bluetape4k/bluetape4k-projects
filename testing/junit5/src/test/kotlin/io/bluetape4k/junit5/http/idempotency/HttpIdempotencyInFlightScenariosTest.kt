package io.bluetape4k.junit5.http.idempotency

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import java.time.Duration

class HttpIdempotencyInFlightScenariosTest {

    @Test
    fun `in-flight scenarios choose one atomic result and reclaim every waiter`() = runSuspendIO {
        val limits = config(maxWaitersPerKey = 2)
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)

        runConformanceScenarios(adapter, limits, inFlightScenarios())

        adapter.completedScenarioCount shouldBeEqualTo 7
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
        adapter.maximumObservedWaiters shouldBeEqualTo 2
    }

    @Test
    fun `one nanosecond timeout preserves before exact and after ordering`() = runSuspendIO {
        val limits = config(waitTimeout = Duration.ofNanos(1))
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)

        runConformanceScenarios(
            adapter = adapter,
            config = limits,
            scenarios = listOf(deadlineOrderingScenario()),
        )

        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }

    @Test
    fun `cancelled waiter completes cleanup after contended state lock is released`() = runSuspendIO {
        val limits = config(maxWaitersPerKey = 1)
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)
        val command = request(idempotencyKeys = listOf("contended-cancellation-key"))
        val owner = async { exchangeChecked(adapter, limits, command) }
        adapter.awaitOwnerStarted(command)
        val waiter = async { exchangeChecked(adapter, limits, command) }
        adapter.awaitWaiterCount(command, 1)
        val cleanupAttempt = adapter.observeNextWaiterCleanupAttempt()

        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val lockHolder = launch { adapter.holdStateLockForTest(entered, release) }
        entered.await()
        waiter.cancel()
        cleanupAttempt.await()
        try {
            waiter.isCompleted.shouldBeFalse()
        } finally {
            release.complete(Unit)
        }

        joinAll(waiter, lockHolder)
        adapter.awaitWaiterCount(command, 0)
        adapter.completeOwner(command, createdResponse())
        owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }

    @Test
    fun `cancelled owner abandons under lock contention and elects one replacement`() = runSuspendIO {
        val limits = config(maxWaitersPerKey = 1)
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)
        val command = request(idempotencyKeys = listOf("contended-owner-cancellation-key"))
        val observedCancellation = CompletableDeferred<CancellationException>()
        val owner = async {
            try {
                exchangeChecked(adapter, limits, command)
            } catch (cancelled: CancellationException) {
                observedCancellation.complete(cancelled)
                throw cancelled
            }
        }
        adapter.awaitOwnerStarted(command)
        val cleanupAttempt = adapter.observeNextOwnerCleanupAttempt()

        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val lockHolder = launch { adapter.holdStateLockForTest(entered, release) }
        entered.await()
        val cancellation = CancellationException("owner disconnected")
        owner.cancel(cancellation)
        cleanupAttempt.await()
        try {
            owner.isCompleted.shouldBeFalse()
        } finally {
            release.complete(Unit)
        }

        joinAll(owner, lockHolder)
        generateSequence<Throwable>(observedCancellation.await()) { failure -> failure.cause }
            .any { failure -> failure === cancellation }
            .shouldBeTrue()
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)

        val retries = List(2) { async { exchangeChecked(adapter, limits, command) } }
        adapter.awaitOwnerStarted(command)
        adapter.awaitWaiterCount(command, 1)
        adapter.sideEffectCount(command) shouldBeEqualTo 2
        adapter.completeOwner(command, createdResponse())
        retries.map { retry ->
            checkNotNull(retry.await().headers["idempotency-replayed"]?.single())
        }.sorted() shouldBeEqualTo listOf("false", "true")
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }

    @Test
    fun `scenario reset releases owner and waiter control observations`() = runSuspendIO {
        val limits = config()
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)
        val pendingOwner = request(idempotencyKeys = listOf("pending-owner-control-key"))
        val ownerObservation = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { adapter.awaitOwnerStarted(pendingOwner) }.exceptionOrNull()
        }

        adapter.resetScenario()
        (ownerObservation.await() is CancellationException).shouldBeTrue()

        val pendingWaiter = request(idempotencyKeys = listOf("pending-waiter-control-key"))
        val owner = async { exchangeChecked(adapter, limits, pendingWaiter) }
        adapter.awaitOwnerStarted(pendingWaiter)
        val waiterObservation = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { adapter.awaitWaiterCount(pendingWaiter, 1) }.exceptionOrNull()
        }

        adapter.resetScenario()
        (waiterObservation.await() is IllegalStateException).shouldBeTrue()
        owner.join()
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }

    @Test
    fun `scenario reset releases a post-commit response delivery hold`() = runSuspendIO {
        val limits = config()
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)
        val command = request(idempotencyKeys = listOf("reset-delivery-hold-key"))
        adapter.holdOwnerResponseDelivery(command)
        val owner = async {
            runCatching { exchangeChecked(adapter, limits, command) }.exceptionOrNull()
        }
        adapter.awaitOwnerStarted(command)
        adapter.completeOwner(command, createdResponse())

        adapter.resetScenario()

        (owner.await() is CancellationException).shouldBeTrue()
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }

    @Test
    fun `transient abandon cannot remove an already committed terminal record`() = runSuspendIO {
        val limits = config()
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)
        val command = request(idempotencyKeys = listOf("terminal-abandon-race-key"))
        val owner = async { exchangeChecked(adapter, limits, command) }
        adapter.awaitOwnerStarted(command)
        adapter.completeOwner(command, createdResponse())
        owner.await() shouldBeEqualTo createdResponse().withReplayFlag(false)

        adapter.abandonOwner(command, transientFailureResponse())

        exchangeChecked(adapter, limits, command) shouldBeEqualTo createdResponse().withReplayFlag(true)
        adapter.sideEffectCount(command) shouldBeEqualTo 1
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }
}
