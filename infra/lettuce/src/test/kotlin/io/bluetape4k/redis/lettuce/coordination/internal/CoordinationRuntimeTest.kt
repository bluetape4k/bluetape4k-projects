package io.bluetape4k.redis.lettuce.coordination.internal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CoordinationRuntimeTest {

    @Test
    fun `runtime enforces registration cap and watchdog service capacity`() = runTest {
        val ticker = MutableTicker()
        val scheduler = RecordingScheduler()
        val runtime = CoordinationRuntime(ticker = ticker, scheduler = scheduler)
        val owner = runtime.registerObject("lock-1")

        repeat(10_000) { generation ->
            owner.registerWatchdog(
                ttl = 3.seconds,
                renewalInterval = 1.seconds,
                generation = generation.toLong(),
            ) { CompletableFuture.completedFuture(CoordinationRenewalOutcome.RENEWED) }
        }

        runtime.activeWatchdogs shouldBeEqualTo 10_000
        assertFailsWith<CoordinationCapacityException> {
            owner.registerWatchdog(3.seconds, 1.seconds, 10_001L) {
                CompletableFuture.completedFuture(CoordinationRenewalOutcome.RENEWED)
            }
        }

        val constrained = CoordinationRuntime(
            ticker = ticker,
            scheduler = RecordingScheduler(),
            limits = CoordinationRuntimeLimits(
                maxRegistrations = 10,
                maxWatchdogsPerTick = 1,
                backlogCadence = 1.seconds,
            ),
        )
        val constrainedOwner = constrained.registerObject("lock-2")
        constrainedOwner.registerWatchdog(3.seconds, 1.seconds, 1L) {
            CompletableFuture.completedFuture(CoordinationRenewalOutcome.RENEWED)
        }
        assertFailsWith<CoordinationCapacityException> {
            constrainedOwner.registerWatchdog(30.seconds, 5.seconds, 2L) {
                CompletableFuture.completedFuture(CoordinationRenewalOutcome.RENEWED)
            }
        }

        owner.close()
        constrainedOwner.close()
    }

    @Test
    fun `due dispatch is capped and backlog drains again within 25 milliseconds`() = runTest {
        val ticker = MutableTicker()
        val scheduler = RecordingScheduler()
        val runtime = CoordinationRuntime(
            ticker = ticker,
            scheduler = scheduler,
            limits = CoordinationRuntimeLimits(
                maxRegistrations = 10,
                maxWatchdogsPerTick = 2,
                backlogCadence = 25.milliseconds,
            ),
        )
        val owner = runtime.registerObject("lock-1")
        var dispatched = 0
        repeat(3) {
            owner.registerTask(Duration.ZERO) { dispatched++ }
        }

        val firstDrain = runtime.drainDue()

        firstDrain.dispatched shouldBeEqualTo 2
        firstDrain.dueBacklog shouldBeEqualTo 1
        firstDrain.nextDrainDelay shouldBeEqualTo 25.milliseconds
        scheduler.scheduledDelays.last() shouldBeEqualTo 25.milliseconds

        val secondDrain = runtime.drainDue()
        secondDrain.dispatched shouldBeEqualTo 1
        secondDrain.dueBacklog shouldBeEqualTo 0
        dispatched shouldBeEqualTo 3
        runtime.activeTasks shouldBeEqualTo 0
        runtime.snapshot().let { snapshot ->
            snapshot.due shouldBeEqualTo 3L
            snapshot.dispatched shouldBeEqualTo 3L
            snapshot.maximumBacklog shouldBeEqualTo 1
        }
    }

    @Test
    fun `object close is idempotent and scheduler ownership is explicit`() = runTest {
        val injectedScheduler = RecordingScheduler()
        val injectedRuntime = CoordinationRuntime(scheduler = injectedScheduler)
        val first = injectedRuntime.registerObject("first")
        val second = injectedRuntime.registerObject("second")
        first.registerTask(1.seconds) {}
        second.registerTask(1.seconds) {}

        first.close()
        first.close()
        injectedRuntime.activeObjects shouldBeEqualTo 1
        injectedRuntime.activeTasks shouldBeEqualTo 1
        injectedScheduler.shutdownCalls shouldBeEqualTo 0

        second.close()
        injectedRuntime.isClosed.shouldBeTrue()
        injectedScheduler.shutdownCalls shouldBeEqualTo 0

        val ownedRuntime = CoordinationRuntime()
        val owned = ownedRuntime.registerObject("owned")
        owned.close()

        ownedRuntime.isClosed.shouldBeTrue()
        ownedRuntime.schedulerShutdown.shouldBeTrue()
    }

    @Test
    fun `connection close terminates registrations and late generations cannot reschedule`() = runTest {
        val ticker = MutableTicker()
        val scheduler = RecordingScheduler()
        val runtime = CoordinationRuntime(ticker = ticker, scheduler = scheduler)
        val owner = runtime.registerObject("lock-1")
        val renewal = CompletableFuture<CoordinationRenewalOutcome>()
        val registration = owner.registerWatchdog(3.seconds, 1.seconds, 7L) { renewal }

        ticker.advance(1.seconds)
        runtime.drainDue().dispatched shouldBeEqualTo 1
        registration.updateGeneration(8L)
        renewal.complete(CoordinationRenewalOutcome.RENEWED)

        runtime.activeWatchdogs shouldBeEqualTo 0
        runtime.snapshot().rejectedLateCompletions shouldBeEqualTo 1L

        val pending = owner.registerTask(1.seconds) {}
        runtime.connectionClosed()

        runtime.isClosed.shouldBeTrue()
        runtime.activeTasks shouldBeEqualTo 0
        runtime.activeWatchdogs shouldBeEqualTo 0
        pending.isClosed.shouldBeTrue()
        scheduler.shutdownCalls shouldBeEqualTo 0
    }

    @Test
    fun `incomplete renewal becomes ownership loss at the Redis ttl boundary`() = runTest {
        val ticker = MutableTicker()
        val runtime = CoordinationRuntime(ticker = ticker, scheduler = RecordingScheduler())
        val owner = runtime.registerObject("lock-1")
        val renewal = CompletableFuture<CoordinationRenewalOutcome>()
        owner.registerWatchdog(3.seconds, 1.seconds, 1L) { renewal }

        ticker.advance(1.seconds)
        runtime.drainDue().dispatched shouldBeEqualTo 1
        runtime.activeWatchdogs shouldBeEqualTo 1

        ticker.advance(2.seconds)
        runtime.drainDue()

        runtime.activeWatchdogs shouldBeEqualTo 0
        runtime.snapshot().missed shouldBeEqualTo 1L
        renewal.isDone.shouldBeFalse()
        owner.close()
    }

    @Test
    fun `renewal completion after its due time records lateness`() = runTest {
        val ticker = MutableTicker()
        val observations = mutableListOf<CoordinationObservation>()
        val runtime = CoordinationRuntime(
            ticker = ticker,
            scheduler = RecordingScheduler(),
            observer = CoordinationObserver(observations::add),
        )
        val owner = runtime.registerObject("lock-1")
        val renewal = CompletableFuture<CoordinationRenewalOutcome>()
        owner.registerWatchdog(3.seconds, 1.seconds, 1L) { renewal }

        ticker.advance(1.seconds)
        runtime.drainDue().dispatched shouldBeEqualTo 1
        ticker.advance(500.milliseconds)
        renewal.complete(CoordinationRenewalOutcome.RENEWED)

        runtime.snapshot().late shouldBeEqualTo 1L
        observations.count { it.name == CoordinationObservationName.WATCHDOG_LATE } shouldBeEqualTo 1
        owner.close()
    }

    @Test
    fun `renewal completion after Redis ttl records ownership loss`() = runTest {
        val ticker = MutableTicker()
        val observations = mutableListOf<CoordinationObservation>()
        val runtime = CoordinationRuntime(
            ticker = ticker,
            scheduler = RecordingScheduler(),
            observer = CoordinationObserver(observations::add),
        )
        val owner = runtime.registerObject("lock-1")
        val renewal = CompletableFuture<CoordinationRenewalOutcome>()
        owner.registerWatchdog(3.seconds, 1.seconds, 1L) { renewal }

        ticker.advance(1.seconds)
        runtime.drainDue().dispatched shouldBeEqualTo 1
        ticker.advance(2.seconds)
        renewal.complete(CoordinationRenewalOutcome.RENEWED)

        runtime.activeWatchdogs shouldBeEqualTo 0
        runtime.snapshot().missed shouldBeEqualTo 1L
        observations.count { it.name == CoordinationObservationName.WATCHDOG_MISSED } shouldBeEqualTo 1
        observations.count { it.name == CoordinationObservationName.OWNERSHIP_LOSS } shouldBeEqualTo 1
        owner.close()
    }

    @Test
    fun `blocking observation sink does not hold the runtime lifecycle lock`() {
        val sinkEntered = CountDownLatch(1)
        val releaseSink = CountDownLatch(1)
        val blockOnce = AtomicBoolean(true)
        val runtime = CoordinationRuntime(
            scheduler = RecordingScheduler(),
            observer = CoordinationObserver { observation ->
                if (
                    observation.name == CoordinationObservationName.OBJECTS &&
                    blockOnce.compareAndSet(true, false)
                ) {
                    sinkEntered.countDown()
                    releaseSink.await(5, TimeUnit.SECONDS)
                }
            },
        )
        val registration = CompletableFuture.runAsync {
            runtime.registerObject("lock-1")
        }

        sinkEntered.await(5, TimeUnit.SECONDS).shouldBeTrue()
        val close = CompletableFuture.runAsync(runtime::connectionClosed)
        try {
            close.get(1, TimeUnit.SECONDS)
        } finally {
            releaseSink.countDown()
            registration.get(5, TimeUnit.SECONDS)
            close.get(5, TimeUnit.SECONDS)
        }
        runtime.isClosed.shouldBeTrue()
    }

    @Test
    fun `runtime registry is weak identity based rather than equals based`() {
        val firstConnection = EqualConnection("same")
        val secondConnection = EqualConnection("same")
        val first = CoordinationRuntime.forConnection(firstConnection, scheduler = RecordingScheduler())
        val sameFirst = CoordinationRuntime.forConnection(firstConnection, scheduler = RecordingScheduler())
        val second = CoordinationRuntime.forConnection(secondConnection, scheduler = RecordingScheduler())

        first.shouldBeSameInstanceAs(sameFirst)
        (first === second).shouldBeFalse()

        first.connectionClosed()
        second.connectionClosed()
    }

    private data class EqualConnection(val value: String)

    private class MutableTicker(private var nowNanos: Long = 0L): MonotonicTicker {
        override fun readNanos(): Long = nowNanos

        fun advance(duration: Duration) {
            nowNanos += duration.inWholeNanoseconds
        }
    }

    private class RecordingScheduler: CoordinationScheduler {
        val scheduledDelays = mutableListOf<Duration>()
        var shutdownCalls: Int = 0
            private set
        override val isShutdown: Boolean get() = shutdownCalls > 0

        override fun schedule(delay: Duration, task: () -> Unit): CoordinationScheduledHandle {
            scheduledDelays += delay
            return CoordinationScheduledHandle { true }
        }

        override fun shutdown() {
            shutdownCalls++
        }
    }
}
