package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRenewalOutcome
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntimeLimits
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockOperation
import io.lettuce.core.RedisConnectionException
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LockWatchdogTest {

    @Test
    fun `fixed lease does not register and watchdog replay registers once until release`() {
        val fixedHarness = TestLockHarness()
        val fixed = LettuceDistributedLock(fixedHarness.client)
        fixed.tryAcquire(
            LockOwnerId.from("fixed-owner"),
            LockRequestId.from("fixed-request"),
            LeasePolicy.Fixed(Duration.ofSeconds(3)),
        ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
        fixedHarness.runtime.activeWatchdogs shouldBeEqualTo 0
        fixed.close()

        val watchdogHarness = TestLockHarness()
        val watchdog = LettuceDistributedLock(watchdogHarness.client)
        val owner = LockOwnerId.from("watchdog-owner")
        val request = LockRequestId.from("watchdog-request")
        val policy = LeasePolicy.Watchdog(
            ttl = Duration.ofSeconds(3),
            renewalInterval = Duration.ofSeconds(1),
            maxLifetime = Duration.ofMinutes(1),
        )
        val first = watchdog.tryAcquire(owner, request, policy)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()

        watchdogHarness.runtime.activeWatchdogs shouldBeEqualTo 1
        watchdog.tryAcquire(owner, request, policy)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
        watchdogHarness.runtime.activeWatchdogs shouldBeEqualTo 1

        watchdog.release(first.handle) shouldBeEqualTo LockMutationResult.Released(0)
        watchdogHarness.runtime.activeWatchdogs shouldBeEqualTo 0
        watchdog.close()
    }

    @Test
    fun `non-final release removes only its request watchdog and keeps the remaining hold renewed`() {
        val harness = TestLockHarness()
        val lock = LettuceDistributedLock(harness.client)
        val owner = LockOwnerId.from("reentrant-watchdog-owner")
        val policy = LeasePolicy.Watchdog(
            ttl = Duration.ofSeconds(3),
            renewalInterval = Duration.ofSeconds(1),
            maxLifetime = Duration.ofMinutes(1),
        )
        val outer = lock.tryAcquire(owner, LockRequestId.from("outer-watchdog"), policy)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle
        val inner = lock.tryAcquire(owner, LockRequestId.from("inner-watchdog"), policy)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle
        harness.runtime.activeWatchdogs shouldBeEqualTo 2

        harness.executor.releaseRemainingHoldCount = 1
        lock.release(outer) shouldBeEqualTo LockMutationResult.Released(1)
        harness.runtime.activeWatchdogs shouldBeEqualTo 1
        harness.ticker.advance(1.seconds)
        harness.runtime.drainDue()

        harness.executor.calls.count { it == DistributedLockOperation.RENEW } shouldBeEqualTo 1
        harness.runtime.activeWatchdogs shouldBeEqualTo 1

        harness.executor.releaseRemainingHoldCount = 0
        lock.release(inner) shouldBeEqualTo LockMutationResult.Released(0)
        harness.runtime.activeWatchdogs shouldBeEqualTo 0
        lock.close()
    }

    @Test
    fun `watchdog capacity after Redis acquisition returns same-request ambiguity for reconciliation`() {
        val harness = TestLockHarness(
            limits = CoordinationRuntimeLimits(maxRegistrations = 1),
        )
        val occupied = harness.registration.registerTask(1.seconds) {}
        val lock = LettuceDistributedLock(harness.client)
        val owner = LockOwnerId.from("capacity-watchdog-owner")
        val request = LockRequestId.from("capacity-watchdog-request")
        val policy = LeasePolicy.Watchdog(
            ttl = Duration.ofSeconds(3),
            renewalInterval = Duration.ofSeconds(1),
            maxLifetime = Duration.ofMinutes(1),
        )

        lock.tryAcquire(owner, request, policy) shouldBeEqualTo LockAcquireResult.Ambiguous(
            owner,
            request,
            LockRecoveryAction.RECONCILE_REQUEST,
        )
        lock.reconcile(owner, request) shouldBeEqualTo
            LockReconcileResult.Ambiguous(LockRecoveryAction.RECONCILE_REQUEST)
        harness.runtime.activeWatchdogs shouldBeEqualTo 0

        occupied.close()
        val reconciled = lock.reconcile(owner, request)
            .shouldBeInstanceOf<LockReconcileResult.Owned<LockHandle>>()
        reconciled.handle.requestId shouldBeEqualTo request
        harness.runtime.activeWatchdogs shouldBeEqualTo 1
        lock.release(reconciled.handle) shouldBeEqualTo LockMutationResult.Released(0)
        lock.close()
    }

    @Test
    fun `watchdog uses bounded early jitter and stops at maximum lifetime`() {
        val ticker = TestTicker()
        val scheduler = ManualCoordinationScheduler()
        val runtime = CoordinationRuntime(
            ticker = ticker,
            scheduler = scheduler,
            watchdogDelay = { interval -> interval * 0.9 },
        )
        val registration = runtime.registerObject("jitter-lock")
        val renewals = AtomicInteger()
        registration.registerWatchdog(
            ttl = 3.seconds,
            renewalInterval = 1.seconds,
            generation = 1L,
            maxLifetime = 2_500.milliseconds,
        ) {
            renewals.incrementAndGet()
            CompletableFuture.completedFuture(CoordinationRenewalOutcome.RENEWED)
        }

        scheduler.scheduledDelays.first() shouldBeGreaterOrEqualTo 900.milliseconds
        scheduler.scheduledDelays.first() shouldBeLessOrEqualTo 1.seconds
        ticker.advance(900.milliseconds)
        runtime.drainDue()
        ticker.advance(900.milliseconds)
        runtime.drainDue()
        ticker.advance(700.milliseconds)
        runtime.drainDue()

        renewals.get() shouldBeEqualTo 2
        runtime.activeWatchdogs shouldBeEqualTo 0
        runtime.snapshot().missed shouldBeEqualTo 0L
        registration.close()
    }

    @Test
    fun `watchdog backend failure records ownership loss without interrupting caller work`() {
        val observations = CopyOnWriteArrayList<LockObservation>()
        val harness = TestLockHarness(
            observationSink = LockObservationSink { observation ->
                observations += observation
                error("observation sink failure")
            },
        )
        harness.executor.asyncRenew = {
            CompletableFuture.failedFuture(RedisConnectionException("renew unavailable"))
        }
        val lock = LettuceDistributedLock(harness.client)
        lock.tryAcquire(
            LockOwnerId.from("failure-owner"),
            LockRequestId.from("failure-request"),
            LeasePolicy.Watchdog(
                ttl = Duration.ofSeconds(3),
                renewalInterval = Duration.ofSeconds(1),
                maxLifetime = Duration.ofMinutes(1),
            ),
        ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()

        harness.ticker.advance(1.seconds)
        harness.runtime.drainDue()

        harness.runtime.activeWatchdogs shouldBeEqualTo 0
        harness.runtime.snapshot().missed shouldBeEqualTo 0L
        observations.filterIsInstance<LockObservation.Counter>()
            .any { it.name == LockCounterName.OWNERSHIP_LOSS_TOTAL }
            .shouldBeEqualTo(true)
        observations.filterIsInstance<LockObservation.Event>()
            .any { it.event.outcome == LockOutcome.OWNERSHIP_LOST }
            .shouldBeEqualTo(true)
        lock.close()
        harness.scheduler.isShutdown.shouldBeEqualTo(false)
    }

    @Test
    fun `accepted watchdog envelope completes renewals without late or missed counts`() {
        val ticker = TestTicker()
        val runtime = CoordinationRuntime(
            ticker = ticker,
            scheduler = ManualCoordinationScheduler(),
            watchdogDelay = { it },
        )
        val registration = runtime.registerObject("accepted-envelope")
        repeat(256) { generation ->
            registration.registerWatchdog(
                ttl = 3.seconds,
                renewalInterval = 1.seconds,
                generation = generation.toLong() + 1L,
                maxLifetime = 1.seconds * 10,
            ) {
                CompletableFuture.completedFuture(CoordinationRenewalOutcome.RENEWED)
            }
        }

        ticker.advance(1.seconds)
        runtime.drainDue().dispatched shouldBeEqualTo 256
        runtime.snapshot().late shouldBeEqualTo 0L
        runtime.snapshot().missed shouldBeEqualTo 0L
        runtime.activeWatchdogs shouldBeEqualTo 256

        registration.close()
        runtime.activeWatchdogs shouldBeEqualTo 0
    }
}
