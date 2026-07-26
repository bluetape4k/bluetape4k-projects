package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduledHandle
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduler
import io.bluetape4k.redis.lettuce.lock.internal.LockWaitSupport
import io.bluetape4k.redis.lettuce.lock.internal.SpinLockRetryPolicy
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.future.await
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration as KotlinDuration

internal class LettuceSpinLockTest {

    @Test
    fun `backoff grows from ten milliseconds to one second and clips to deadline`() {
        val policy = SpinLockRetryPolicy(SpinLockConfig(jitterRatio = 0.0))

        policy.delay(1, Long.MAX_VALUE) shouldBeEqualTo Duration.ofMillis(10)
        policy.delay(2, Long.MAX_VALUE) shouldBeEqualTo Duration.ofMillis(20)
        policy.delay(3, Long.MAX_VALUE) shouldBeEqualTo Duration.ofMillis(40)
        policy.delay(20, Long.MAX_VALUE) shouldBeEqualTo Duration.ofSeconds(1)
        policy.delay(3, Duration.ofMillis(15).toNanos()) shouldBeEqualTo Duration.ofMillis(15)
    }

    @Test
    fun `jitter stays within zero to twenty five percent and zero disables the source`() {
        SpinLockRetryPolicy(
            SpinLockConfig(jitterRatio = 0.25),
            jitterSource = { 0.0 },
        ).delay(1, Long.MAX_VALUE) shouldBeEqualTo Duration.ofMillis(10)
        SpinLockRetryPolicy(
            SpinLockConfig(jitterRatio = 0.25),
            jitterSource = { 1.0 },
        ).delay(1, Long.MAX_VALUE) shouldBeEqualTo Duration.ofMillis(12).plusNanos(500_000)
        SpinLockRetryPolicy(
            SpinLockConfig(jitterRatio = 0.0),
            jitterSource = { error("disabled jitter must not sample") },
        ).delay(2, Long.MAX_VALUE) shouldBeEqualTo Duration.ofMillis(20)
    }

    @Test
    fun `attempt rate is capped and invalid multipliers fail closed`() {
        val policy = SpinLockRetryPolicy(
            SpinLockConfig(
                initialDelay = Duration.ofMillis(1),
                multiplier = 1.0,
                maxAttemptsPerSecond = 100,
            ),
            jitterSource = { 0.0 },
        )

        policy.delay(1, Long.MAX_VALUE) shouldBeEqualTo Duration.ofMillis(10)
        assertFailsWith<IllegalArgumentException> { SpinLockConfig(multiplier = 0.99) }
        assertFailsWith<IllegalArgumentException> { SpinLockConfig(multiplier = Double.NaN) }
    }

    @Test
    fun `future cancellation removes the scheduled retry before another attempt`() {
        val ticker = TestTicker()
        val scheduler = AdvancingCoordinationScheduler(ticker)
        val runtime = CoordinationRuntime(ticker = ticker, scheduler = scheduler)
        val registration = runtime.registerObject("spin-wait-test")
        val support = LockWaitSupport(
            registration = registration,
            isClosed = { false },
            retryPolicy = SpinLockRetryPolicy(SpinLockConfig(jitterRatio = 0.0)),
            ticker = ticker,
        )
        var attempts = 0

        val pending = support.acquireAsync(Duration.ofSeconds(1)) {
            attempts++
            CompletableFuture.completedFuture(LockAcquireResult.Contended(1_000))
        }
        scheduler.runNext()
        attempts shouldBeEqualTo 1

        pending.cancel(false) shouldBeEqualTo true
        scheduler.runNext()
        attempts shouldBeEqualTo 1
        support.close()
        registration.close()
    }

    @Test
    fun `suspend cancellation removes the scheduled retry before another attempt`() = runSuspendIO {
        val ticker = TestTicker()
        val scheduler = AdvancingCoordinationScheduler(ticker)
        val runtime = CoordinationRuntime(ticker = ticker, scheduler = scheduler)
        val registration = runtime.registerObject("spin-suspend-test")
        val support = LockWaitSupport(
            registration = registration,
            isClosed = { false },
            retryPolicy = SpinLockRetryPolicy(SpinLockConfig(jitterRatio = 0.0)),
            ticker = ticker,
        )
        var attempts = 0

        val pending = async {
            support.acquireSuspending(Duration.ofSeconds(1)) {
                attempts++
                LockAcquireResult.Contended(1_000)
            }
        }
        while (attempts == 0) yield()
        attempts shouldBeEqualTo 1

        pending.cancelAndJoin()
        scheduler.runNext()
        attempts shouldBeEqualTo 1
        support.close()
        registration.close()
    }

    @Test
    fun `spin lock preserves exclusive reentry and bounded timeout on the distributed schema`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceSpinLock.create(
                connection,
                "spin-contract-${System.nanoTime()}",
                SpinLockConfig(jitterRatio = 0.0),
            )
            try {
                val first = lock.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
                    .handle
                val second = lock.tryAcquire(OWNER_1, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Reentered<LockHandle>>()
                    .handle

                lock.acquire(
                    OWNER_2,
                    REQUEST_3,
                    Duration.ofMillis(35),
                    LEASE,
                ) shouldBeEqualTo LockAcquireResult.TimedOut

                lock.release(second) shouldBeEqualTo LockMutationResult.Released(1)
                lock.release(first) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                lock.close()
            }
        }
    }

    private class AdvancingCoordinationScheduler(
        private val ticker: TestTicker,
    ): CoordinationScheduler {
        private val tasks = CopyOnWriteArrayList<ScheduledTask>()
        override val isShutdown: Boolean = false

        override fun schedule(
            delay: KotlinDuration,
            task: () -> Unit,
        ): CoordinationScheduledHandle {
            val scheduled = ScheduledTask(delay, task)
            tasks += scheduled
            return CoordinationScheduledHandle {
                scheduled.cancelled = true
                true
            }
        }

        fun runNext() {
            val scheduled = tasks.removeFirst()
            ticker.advance(scheduled.delay)
            if (!scheduled.cancelled) scheduled.task()
        }

        override fun shutdown() = Unit
    }

    private data class ScheduledTask(
        val delay: KotlinDuration,
        val task: () -> Unit,
        @Volatile var cancelled: Boolean = false,
    )

    private companion object {
        val OWNER_1 = LockOwnerId.from("spin-owner-1")
        val OWNER_2 = LockOwnerId.from("spin-owner-2")
        val REQUEST_1 = LockRequestId.from("spin-request-1")
        val REQUEST_2 = LockRequestId.from("spin-request-2")
        val REQUEST_3 = LockRequestId.from("spin-request-3")
        val LEASE = LeasePolicy.Fixed(Duration.ofSeconds(3))
    }
}

internal class BlockingLettuceSpinLockContractTest: LockContract() {
    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        name: String,
        config: LockConfig,
    ): DistributedLockAdapter =
        blockingSpinAdapter(
            LettuceSpinLock.create(connection, name, SpinLockConfig(lock = config, jitterRatio = 0.0)),
        )
}

internal class FutureLettuceSpinLockContractTest: LockContract() {
    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        name: String,
        config: LockConfig,
    ): DistributedLockAdapter =
        futureSpinAdapter(
            LettuceSpinLock.create(connection, name, SpinLockConfig(lock = config, jitterRatio = 0.0)),
        )
}

internal class SuspendLettuceSpinLockContractTest: LockContract() {
    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        name: String,
        config: LockConfig,
    ): DistributedLockAdapter =
        suspendSpinAdapter(
            LettuceSuspendSpinLock.create(connection, name, SpinLockConfig(lock = config, jitterRatio = 0.0)),
        )
}

private fun blockingSpinAdapter(lock: LettuceSpinLock): DistributedLockAdapter =
    object: DistributedLockAdapter {
        override suspend fun tryAcquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ) = lock.tryAcquire(ownerId, requestId, leasePolicy)

        override suspend fun acquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ) = lock.acquire(ownerId, requestId, waitTime, leasePolicy)

        override suspend fun inspect(handle: LockHandle) = lock.inspect(handle)

        override suspend fun reconcile(ownerId: LockOwnerId, requestId: LockRequestId) =
            lock.reconcile(ownerId, requestId)

        override suspend fun renew(handle: LockHandle, extension: Duration) = lock.renew(handle, extension)

        override suspend fun release(handle: LockHandle) = lock.release(handle)

        override fun close() = lock.close()
    }

private fun futureSpinAdapter(lock: LettuceSpinLock): DistributedLockAdapter =
    object: DistributedLockAdapter {
        override suspend fun tryAcquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ) = lock.tryAcquireAsync(ownerId, requestId, leasePolicy).await()

        override suspend fun acquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ) = lock.acquireAsync(ownerId, requestId, waitTime, leasePolicy).await()

        override suspend fun inspect(handle: LockHandle) = lock.inspectAsync(handle).await()

        override suspend fun reconcile(ownerId: LockOwnerId, requestId: LockRequestId) =
            lock.reconcileAsync(ownerId, requestId).await()

        override suspend fun renew(handle: LockHandle, extension: Duration) =
            lock.renewAsync(handle, extension).await()

        override suspend fun release(handle: LockHandle) = lock.releaseAsync(handle).await()

        override fun close() = lock.close()
    }

private fun suspendSpinAdapter(lock: LettuceSuspendSpinLock): DistributedLockAdapter =
    object: DistributedLockAdapter {
        override suspend fun tryAcquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ) = lock.tryAcquire(ownerId, requestId, leasePolicy)

        override suspend fun acquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ) = lock.acquire(ownerId, requestId, waitTime, leasePolicy)

        override suspend fun inspect(handle: LockHandle) = lock.inspect(handle)

        override suspend fun reconcile(ownerId: LockOwnerId, requestId: LockRequestId) =
            lock.reconcile(ownerId, requestId)

        override suspend fun renew(handle: LockHandle, extension: Duration) = lock.renew(handle, extension)

        override suspend fun release(handle: LockHandle) = lock.release(handle)

        override fun close() = lock.close()
    }
