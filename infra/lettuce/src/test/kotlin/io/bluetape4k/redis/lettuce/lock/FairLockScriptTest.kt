package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeZero
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.FairLockClient
import io.bluetape4k.redis.lettuce.lock.internal.FairWaiterIdentity
import io.bluetape4k.redis.lettuce.lock.internal.deriveFairLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.fairWaiterMember
import io.lettuce.core.ScoredValue
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

internal class FairLockScriptTest : AbstractLettuceTest() {

    private lateinit var connection: StatefulRedisConnection<String, String>
    private lateinit var commands: RedisCommands<String, String>
    private lateinit var lock: FairLockClient
    private lateinit var name: String

    private val lease = LeasePolicy.Fixed(Duration.ofSeconds(3))

    @BeforeEach
    fun setUp() {
        connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        commands = connection.sync()
        name = "fair-script-${randomName().substringAfter(':')}"
        lock = FairLockClient.create(connection, name, FairLockConfig())
        deleteKeys()
    }

    @AfterEach
    fun tearDown() {
        try {
            lock.close()
            deleteKeys()
        } finally {
            connection.close()
        }
    }

    @Test
    fun `Redis enqueue sequence defines FIFO admission`() {
        val holder = acquire("holder", "holder-request")
        val second = enqueue("second", "second-request")
        val third = enqueue("third", "third-request")

        third.enqueueSequence shouldBeGreaterThan second.enqueueSequence
        lock.release(holder) shouldBeEqualTo LockMutationResult.Released(0)

        lock.tryAcquire(owner("third"), request("third-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Contended>()
        lock.tryAcquire(owner("second"), request("second-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
    }

    @Test
    fun `reentry bypasses no queued waiter and creates no second queue member`() {
        val holder = acquire("holder", "holder-request")
        enqueue("waiter", "waiter-request")
        val keys = deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8)
        val queuedBefore = commands.zcard(keys.queue)

        lock.tryAcquire(owner("holder"), request("holder-reentry"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Reentered<LockHandle>>()
        commands.zcard(keys.queue) shouldBeEqualTo queuedBefore

        lock.release(holder)
    }

    @Test
    fun `queued request follows its owner after another request becomes queue head`() {
        val holder = acquire("holder", "holder-request")
        val sharedOwner = owner("shared-owner")
        enqueue(sharedOwner, request("first-request"))
        enqueue(sharedOwner, request("second-request"))
        val keys = deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8)
        commands.zcard(keys.queue) shouldBeEqualTo 2L

        lock.release(holder) shouldBeEqualTo LockMutationResult.Released(0)
        lock.tryAcquire(sharedOwner, request("first-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
        lock.tryAcquire(sharedOwner, request("second-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Reentered<LockHandle>>()

        commands.zcard(keys.queue).shouldBeZero()
    }

    @Test
    fun `same request under different owners remains two exact waiter identities`() {
        val holder = acquire("holder", "holder-request")
        val shared = request("shared-request")
        val first = enqueue(owner("first"), shared)
        val second = enqueue(owner("second"), shared)
        val keys = deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8)

        second.enqueueSequence shouldBeGreaterThan first.enqueueSequence
        commands.zcard(keys.queue) shouldBeEqualTo 2L
        lock.reconcile(owner("first"), shared)
            .shouldBeInstanceOf<LockReconcileResult.Queued>()
            .waiter.enqueueSequence shouldBeEqualTo first.enqueueSequence
        lock.reconcile(owner("second"), shared)
            .shouldBeInstanceOf<LockReconcileResult.Queued>()
            .waiter.enqueueSequence shouldBeEqualTo second.enqueueSequence

        lock.release(holder)
    }

    @Test
    fun `timeout removes only its exact waiter`() {
        val holder = acquire("holder", "holder-request")
        val shared = request("shared-timeout")
        enqueue(owner("survivor"), shared)

        lock.acquire(
            owner("timed-out"),
            shared,
            Duration.ofMillis(25),
            lease,
        ) shouldBeEqualTo LockAcquireResult.TimedOut

        lock.reconcile(owner("timed-out"), shared) shouldBeEqualTo LockReconcileResult.NotFound
        lock.reconcile(owner("survivor"), shared).shouldBeInstanceOf<LockReconcileResult.Queued>()
        lock.release(holder)
    }

    @Test
    fun `queue capacity ten thousand rejects without mutating existing waiters`() {
        val holder = acquire("holder", "holder-request")
        val keys = deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8)
        val deadline = System.currentTimeMillis() + Duration.ofMinutes(1).toMillis()
        val waiters = (1..10_000).associate { index ->
            "seed-$index" to "$index|1|$deadline"
        }
        val queue = (1..10_000).map { index ->
            ScoredValue.just(index.toDouble(), "seed-$index")
        }
        commands.hset(keys.waiters, waiters)
        commands.zadd(keys.queue, *queue.toTypedArray())
        commands.set(keys.sequence, "10000")

        lock.enqueueOnce(
            owner("overflow"),
            request("overflow-request"),
            Duration.ofSeconds(1),
            lease,
        ) shouldBeEqualTo LockAcquireResult.CapacityExceeded
        commands.zcard(keys.queue) shouldBeEqualTo 10_000L

        lock.release(holder)
    }

    @Test
    fun `same owner and request replay retains one waiter and sequence`() {
        val holder = acquire("holder", "holder-request")
        val owner = owner("waiter")
        val request = request("waiter-request")
        val first = enqueue(owner, request)
        val second = lock.reconcile(owner, request)
            .shouldBeInstanceOf<LockReconcileResult.Queued>()
            .waiter
        val keys = deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8)

        second.enqueueSequence shouldBeEqualTo first.enqueueSequence
        commands.zcard(keys.queue) shouldBeEqualTo 1L
        lock.release(holder)
    }

    @Test
    fun `default cleanup batch fails closed without bypassing the remaining stale head`() {
        assertBoundedCleanup(64)
    }

    @Test
    fun `maximum cleanup batch fails closed without bypassing the remaining stale head`() {
        lock.close()
        lock = FairLockClient.create(connection, name, FairLockConfig(cleanupBatchSize = 256))
        assertBoundedCleanup(256)
    }

    @Test
    fun `stale compare delete cannot remove a newer waiter generation`() {
        val holder = acquire("holder", "holder-request")
        val owner = owner("replaceable")
        val request = request("replaceable-request")
        val first = enqueue(owner, request)
        val keys = deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8)
        val member = fairWaiterMember(owner, request)
        val stored = commands.hget(keys.waiters, member).split('|')
        val firstIdentity = FairWaiterIdentity(stored[0].toLong(), stored[1].toLong())
        val newerGeneration = firstIdentity.generation + 1L
        commands.hset(keys.waiters, member, "${first.enqueueSequence}|$newerGeneration|${stored[2]}")

        lock.removeWaiter(owner, request, firstIdentity) shouldBeEqualTo
            LockReconcileResult.StaleGeneration
        lock.reconcile(owner, request)
            .shouldBeInstanceOf<LockReconcileResult.Queued>()
            .waiter.enqueueSequence shouldBeEqualTo first.enqueueSequence
        commands.hget(keys.waiters, member).split('|')[1].toLong() shouldBeEqualTo newerGeneration
        lock.release(holder)
    }

    @Test
    fun `malformed cleanup candidate fails closed before deleting earlier stale waiters`() {
        val keys = deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8)
        commands.zadd(keys.queue, 1.0, "stale")
        commands.zadd(keys.queue, 2.0, "malformed")
        commands.hset(keys.waiters, "stale", "1|0|1")
        commands.hset(keys.waiters, "malformed", "not-a-waiter")
        commands.set(keys.sequence, "2")

        lock.tryAcquire(owner("candidate"), request("candidate-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>()
        commands.zcard(keys.queue) shouldBeEqualTo 2L
        commands.hlen(keys.waiters) shouldBeEqualTo 2L
    }

    @Test
    fun `malformed terminal state fails closed without acquisition mutation`() {
        val keys = deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8)
        commands.hset(keys.terminal, mapOf("owner" to "owner", "generation" to "bad", "request" to "request"))
        commands.pexpire(keys.terminal, Duration.ofMinutes(1).toMillis())

        lock.tryAcquire(owner("candidate"), request("candidate-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>()
        commands.exists(keys.state, keys.holds).shouldBeZero()
        commands.hget(keys.terminal, "generation") shouldBeEqualTo "bad"
    }

    private fun assertBoundedCleanup(batchSize: Int) {
        val holder = acquire("holder", "holder-request")
        lock.release(holder) shouldBeEqualTo LockMutationResult.Released(0)
        val config = FairLockConfig(cleanupBatchSize = batchSize)
        val keys = deriveFairLockKeys(name, config, StringCodec.UTF8)
        val staleCount = batchSize + 1
        val waiters = (1..staleCount).associate { index -> "stale-$index" to "$index|1|1" }
        val queue = (1..staleCount).map { index ->
            ScoredValue.just(index.toDouble(), "stale-$index")
        }
        commands.hset(keys.waiters, waiters)
        commands.zadd(keys.queue, *queue.toTypedArray())
        commands.set(keys.sequence, staleCount.toString())

        lock.tryAcquire(owner("candidate"), request("candidate-request"), lease) shouldBeEqualTo
            LockAcquireResult.CleanupPending
        commands.exists(keys.state).shouldBeZero()
        commands.zcard(keys.queue) shouldBeEqualTo 1L

        lock.tryAcquire(owner("candidate"), request("candidate-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
        commands.zcard(keys.queue).shouldBeZero()
    }

    private fun acquire(owner: String, request: String): LockHandle =
        lock.tryAcquire(owner(owner), request(request), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle

    private fun enqueue(owner: String, request: String): FairWaiterState =
        enqueue(owner(owner), request(request))

    private fun enqueue(owner: LockOwnerId, request: LockRequestId): FairWaiterState {
        lock.enqueueOnce(owner, request, Duration.ofSeconds(2), lease)
            .shouldBeInstanceOf<LockAcquireResult.Contended>()
        return eventuallyQueued(owner, request)
    }

    private fun eventuallyQueued(owner: LockOwnerId, request: LockRequestId): FairWaiterState {
        repeat(100) {
            val reconciled = lock.reconcile(owner, request)
            if (reconciled is LockReconcileResult.Queued) return reconciled.waiter
            Thread.onSpinWait()
        }
        error("waiter was not enqueued")
    }

    private fun owner(value: String) = LockOwnerId.from(value)

    private fun request(value: String) = LockRequestId.from(value)

    private fun deleteKeys() {
        val keys = deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8)
        commands.del(*keys.all)
        commands.exists(*keys.all).shouldBeZero()
    }
}
